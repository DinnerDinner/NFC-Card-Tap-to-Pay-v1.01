import random
from datetime import date, datetime
from fastapi import FastAPI, Depends, HTTPException, status
from pydantic import BaseModel, EmailStr
from sqlalchemy.orm import Session

from db_files.database import SessionLocal, engine, Base
from db_files.models import User
from pydantic import BaseModel, EmailStr, validator
from datetime import date
import re




Base.metadata.create_all(bind=engine)
app = FastAPI()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()




def clean_phone_number(phone_number: str) -> str:
    return re.sub(r'\D', '', phone_number)



class UserRegistrationPayload(BaseModel):
    first_name: str
    last_name: str
    email: EmailStr
    phone_number: str
    dob: date
    password: str 
    card_uid: str

@app.post("/register_user/")
def register_user(payload: UserRegistrationPayload, db: Session = Depends(get_db)):
    clean_phone_number = clean_phone_number(payload.phone_number)
    if db.query(User).filter(User.email == payload.email).first():
        raise HTTPException(status_code=400, detail="Email already exists")
    if db.query(User).filter(User.phone_number == clean_phone_number).first():
        raise HTTPException(status_code=400, detail="Phone number already exists")

    balance_cents = random.randint(0, 100000)
    email = payload.email.lower()
    user = User(
        first_name=payload.first_name,
        last_name=payload.last_name,
        email=email,
        phone_number=clean_phone_number,
        dob=payload.dob,
        password_hash=payload.password,  
        card_uid=payload.card_uid,
        is_cadet=True, 
        balance_cents=balance_cents,
        status="active",
        last_login=None,
        is_student = True,
        is_hospital_user = False
    )

    db.add(user)
    db.commit()
    db.refresh(user)

    return {
        "message": "✅ User signedup successfully. Restart app",
        "user_id": user.id,
        "balance_dollars": balance_cents / 100
    }






class UserLoginPayload(BaseModel):
    email: EmailStr
    password: str

@app.post("/login")
def login(payload: UserLoginPayload, db: Session = Depends(get_db)):
    email = payload.email.lower()
    user = db.query(User).filter(User.email == email).first()
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid email or password")
    if user.password_hash != payload.password:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid email or password")

    balance_dollars = user.balance_cents / 100

    return {
        "user_id": user.id,
        "first_name": user.first_name,
        "last_name": user.last_name,
        "email": user.email,
        "phone_number": user.phone_number,
        "dob": user.dob.isoformat() if user.dob else None,
        "balance": balance_dollars,
        "message": f"Welcome back, {user.first_name}!"
    }






@app.post("/profile")
def get_profile(payload: dict, db: Session = Depends(get_db)):
    email = payload.get("email")
    user = db.query(User).filter(User.email == email).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return {
        "first_name": user.first_name,
        "last_name": user.last_name,
        "email": user.email,
        "phone_number": user.phone_number,
        "dob": user.dob.isoformat() if user.dob else None,
        "balance": user.balance_cents / 100,
        "card_uid": user.card_uid, 
        "is_cadet": "Yes" if user.is_cadet else "No",
        "is_student": "Yes" if user.is_student else "No",
        "is_hospital_user": "Yes" if user.is_hospital_user else "No"
    }




class UserUpdatePayload(BaseModel):
    first_name: str
    last_name: str
    email: EmailStr
    phone_number: str
    dob: date
    card_uid: str
    is_cadet: bool = False
    is_student: bool = False
    is_hospital_user: bool = False

    @validator("phone_number")
    def validate_phone_number(cls, v):
        if len(re.sub(r'\D', '', v)) < 7:  # At least 7 digits
            raise ValueError("Phone number too short")
        return v

    @validator("dob")
    def validate_dob(cls, v):
        if v > date.today():
            raise ValueError("Date of birth cannot be in the future")
        return v

from fastapi import Body

@app.post("/update-profile")
def update_profile(payload: UserUpdatePayload = Body(...), db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == payload.email).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    # Check for unique email if email changed - (if email can be changed, adjust logic)
    # Here, we assume email is unique and fixed; if changeable, you'd handle it differently.

    # Check phone_number uniqueness (exclude current user)
    phone_number = clean_phone_number(payload.phone_number)
    phone_exists = db.query(User).filter(User.phone_number == phone_number, User.id != user.id).first()
    if phone_exists:
        raise HTTPException(status_code=400, detail="Phone number already exists")

    # Check card_uid uniqueness (exclude current user)
    card_exists = db.query(User).filter(User.card_uid == payload.card_uid, User.id != user.id).first()
    if card_exists:
        raise HTTPException(status_code=400, detail="Card UID already exists")

    # Update fields
    user.first_name = payload.first_name
    user.last_name = payload.last_name
    # user.email = payload.email  # Uncomment if email is changeable
    user.phone_number = phone_number
    user.dob = payload.dob
    user.card_uid = payload.card_uid
    user.is_cadet = payload.is_cadet
    user.is_student = payload.is_student
    user.is_hospital_user = payload.is_hospital_user

    db.commit()
    db.refresh(user)

    return {
        "message": "✅ Profile updated successfully",
        "first_name": user.first_name,
        "last_name": user.last_name,
        "email": user.email,
        "phone_number": user.phone_number,
        "dob": user.dob.isoformat() if user.dob else None,
        "balance": user.balance_cents / 100,
        "card_uid": user.card_uid,
        "is_cadet": "Yes" if user.is_cadet else "No",
        "is_student": "Yes" if user.is_student else "No",
        "is_hospital_user": "Yes" if user.is_hospital_user else "No"
    }






class PurchasePayload(BaseModel):
    uid: str              # tapped‑card UID  (customer)
    merchant_email: EmailStr
    amount: float         # CAD dollars (positive)

@app.post("/transfer")
def transfer(payload: PurchasePayload, db: Session = Depends(get_db)):
    """
    1.  Find customer by physical‑card UID  (case‑exact).
    2.  Find merchant by e‑mail (case‑insensitive).
    3.  Move `amount` dollars from customer to merchant.
    """
    if payload.amount <= 0:
        raise HTTPException(status_code=400, detail="❌ Payment failed\nAmount must be positive")
    customer = db.query(User).filter(User.card_uid == payload.uid).first()
    if not customer:
        raise HTTPException(status_code=404, detail="❌ Payment failed\nCustomer card not found")
    merchant_email = payload.merchant_email.lower()
    merchant = db.query(User).filter(User.email == merchant_email).first()
    if not merchant:
        raise HTTPException(status_code=404, detail="❌ Payment failed\nMerchant not found")
    cents = int(round(payload.amount * 100))
    if customer.card_uid == merchant.card_uid:
        raise HTTPException(status_code=400, detail="❌ Payment failed\nYou may not tap your own card")
    if customer.balance_cents < cents:
        raise HTTPException(status_code=400, detail="❌ Payment failed\nCustomer has insufficient funds")

    customer.balance_cents -= cents
    merchant.balance_cents += cents
    db.commit()

    return {
        "message": f"✅ ${payload.amount:.2f} transferred "
                   f"from {customer.first_name} to {merchant.first_name}.",
        "customer_balance": customer.balance_cents / 100,
        "merchant_balance": merchant.balance_cents / 100
    }



print("V1 Achieved")