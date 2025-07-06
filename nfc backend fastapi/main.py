import random
from datetime import date, datetime
from fastapi import FastAPI, Depends, HTTPException
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

    # Create user object
    user = User(
        first_name=payload.first_name,
        last_name=payload.last_name,
        email=payload.email,
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








def get_or_create_user_by_card_uid(uid: str, db: Session) -> tuple[User, bool]:
    user = db.query(User).filter(User.card_uid == uid).first()
    if user:
        return user, True

    # Create new user with simulated data
    new_balance = random.randint(0, 100000)  # cents
    first_name = random.choice(["Alice", "Bob", "Charlie", "Diana", "Eve", "Frank"])
    last_name = random.choice(["Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia"])
    email = f"{first_name.lower()}.{last_name.lower()}{random.randint(1,999)}@example.com"
    phone_number = f"+1000000{random.randint(1000,9999)}"
    dob = random_date()
    password_hash = "hashedpasswordplaceholder"  # replace with real hashing later

    user = User(
        card_uid=uid,
        balance_cents=new_balance,
        first_name=first_name,
        last_name=last_name,
        email=email,
        phone_number=phone_number,
        dob=dob,
        password_hash=password_hash,
        is_cadet=False,
        is_hospital_user=False,
        is_student=False,
        status="active",
        last_login=None,
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user, False



@app.post("/nfc-tap")
def nfc_tap(payload: dict, db: Session = Depends(get_db)):
    uid = payload.get("uid")


    user, existing = get_or_create_user_by_card_uid(uid, db)

    # Prepare balance in dollars from cents
    balance_dollars = user.balance_cents / 100

    return {
        "uid": user.card_uid,
        "first_name": user.first_name,
        "last_name": user.last_name,
        "email": user.email,
        "phone_number": user.phone_number,
        "dob": user.dob.isoformat() if user.dob else None,
        "balance": balance_dollars,
        "existing_user": existing,
        "message": (
            f"Welcome back, {user.first_name}! Card ending in {user.card_uid[-4:]} balance ${balance_dollars:.2f}"
            if existing
            else f"New user {user.first_name} created with starting balance ${balance_dollars:.2f}"
        )
    }

print("V2 Achieved")







