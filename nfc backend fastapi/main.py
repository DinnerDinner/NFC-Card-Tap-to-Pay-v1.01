import random
from datetime import date, datetime
from fastapi import FastAPI, Depends, HTTPException, status
from pydantic import BaseModel, EmailStr
from sqlalchemy.orm import Session

from db_files.database import SessionLocal, engine, Base
from db_files.models import User

# Initialize DB
Base.metadata.create_all(bind=engine)
app = FastAPI()

# -------------------- DB Dependency --------------------

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# -------------------- Utility Functions --------------------

def random_date(start_year=1970, end_year=2005):
    year = random.randint(start_year, end_year)
    month = random.randint(1, 12)
    day = random.randint(1, 28)  # safe dates
    return date(year, month, day)

# -------------------- Pydantic Model --------------------

class UserRegistrationPayload(BaseModel):
    first_name: str
    last_name: str
    email: EmailStr
    phone_number: str
    dob: date
    password: str 
    card_uid: str


# -------------------- Real Form-Based Registration --------------------

@app.post("/register_user/")
def register_user(payload: UserRegistrationPayload, db: Session = Depends(get_db)):
    # Check if user already exists
    if db.query(User).filter(User.email == payload.email).first():
        raise HTTPException(status_code=400, detail="Email already exists")
    if db.query(User).filter(User.phone_number == payload.phone_number).first():
        raise HTTPException(status_code=400, detail="Phone number already exists")

    # Simulate balance
    balance_cents = random.randint(0, 100000)
    email = payload.email.lower()
    # Create user object
    user = User(
        first_name=payload.first_name,
        last_name=payload.last_name,
        email=email,
        phone_number=payload.phone_number,
        dob=payload.dob,
        password_hash=payload.password,  
        card_uid=payload.card_uid,
        is_cadet=True,  # For now, default to cadet
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
        "message": "✅ User created successfully.",
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
    # For now, password is stored as plain text (not secure), do direct compare
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


class PurchasePayload(BaseModel):
    uid: str              # tapped‑card UID  (customer)
    merchant_email: EmailStr
    amount: float         # CAD dollars (positive)

@app.post("/purchase")
def purchase(payload: PurchasePayload, db: Session = Depends(get_db)):
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
        raise HTTPException(status_code=400, detail="❌ Payment failed\nMerchant may not tap their own card")
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


print("V2 Achieved")







