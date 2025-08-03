print("V2 Started!! WITH MPOS SYSTEM!!!")
from datetime import datetime, time
import asyncio
# from apscheduler.schedulers.background import BackgroundScheduler
# from apscheduler.triggers.cron import CronTrigger
from fastapi import UploadFile, File
# from cloudinary_utils import upload_image_to_cloudinary
from typing import Annotated
from pydantic import BaseModel, Field
from decimal import Decimal
import random
from datetime import date, datetime
from fastapi import FastAPI, Depends, HTTPException, status
from pydantic import BaseModel, EmailStr
from sqlalchemy.orm import Session
from fastapi import Request
from nfc_backend_fastapi.db_files.database import SessionLocal, engine, Base
from nfc_backend_fastapi.db_files.models import User, Business, Product

from pydantic import BaseModel, EmailStr, validator
from datetime import date
import re


from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from pydantic import BaseModel
from fastapi import FastAPI, UploadFile, File, HTTPException, Depends, Body
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session
from decimal import Decimal
from typing import List, Optional, Annotated
import cloudinary
import cloudinary.uploader


Base.metadata.create_all(bind=engine)
app = FastAPI()

cloudinary.config(
    cloud_name="dqhr9pgl0",
    api_key="289124826863297",
    api_secret="ZscnuDZzhq4uYAA2_qH6SIDTHkg"
)

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
    cleanup_phone_number = clean_phone_number(payload.phone_number)
    if db.query(User).filter(User.email == payload.email).first():
        raise HTTPException(status_code=400, detail="Email already exists")
    if db.query(User).filter(User.phone_number == cleanup_phone_number).first():
        raise HTTPException(status_code=400, detail="Phone number already exists")

    balance_cents = random.randint(0, 100000)
    email = payload.email.lower()
    user = User(
        first_name=payload.first_name,
        last_name=payload.last_name,
        email=email,
        phone_number=cleanup_phone_number,
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
        "message": "✅ User SignedUp successfully. Please LogIn",
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
    user_id = payload.get("user_id")
    user = db.query(User).filter(User.id == user_id).first()
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
    user_id: int
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
    user = db.query(User).filter(User.id == payload.user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    # Check for unique email if email changed - (if email can be changed, adjust logic)
    if user.email != payload.email:
        email_exists = db.query(User).filter(User.email == payload.email).first()
        if email_exists:
            raise HTTPException(status_code=400, detail="Email already exists")


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
    user.email = payload.email  # Uncomment if email is changeable
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
    merchant_id: int
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
    merchant_id = payload.merchant_id
    merchant = db.query(User).filter(User.id == merchant_id).first()
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





class BusinessSetupPayload(BaseModel):
    user_id: int
    business_name: str

class BusinessSetupResponse(BaseModel):
    business_id: int
    business_name: str
    owner_email: EmailStr
    message: str


@app.post("/business/setup", response_model=BusinessSetupResponse)
def setup_business(payload: BusinessSetupPayload, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == payload.user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    existing_business = db.query(Business).filter(Business.owner_id == user.id).first()
    if existing_business:
        return BusinessSetupResponse(
            business_id=existing_business.id,
            business_name=existing_business.business_name,
            owner_email=user.email,
            message="Business already exists for this user"
        )

    new_business = Business(
        owner_id=user.id,
        business_name=payload.business_name
    )
    db.add(new_business)
    db.commit()
    db.refresh(new_business)

    return BusinessSetupResponse(
        business_id=new_business.id,
        business_name=new_business.business_name,
        owner_email=user.email,
        message="Business successfully created"
    )

@app.post("/business/exists")
def check_business(payload: dict, db: Session = Depends(get_db)):
    user_id = payload.get("user_id")
    if not user_id:
        raise HTTPException(status_code=400, detail="user_id required")

    business = db.query(Business).filter(Business.owner_id == user_id).first()
    if business:
        return {
            "has_business": True,
            "business_name": business.business_name,
            "business_id": business.id,
        }
    return { "has_business": False }






class ProductsListRequest(BaseModel):
    user_id: int

class ProductOut(BaseModel):
    id: int
    title: str
    price: float
    sku: str | None = None
    barcode_number: str | None = None
    description: str | None = None
    keywords: str | None = None
    image_url: str | None = None


    class Config:
        orm_mode = True


@app.post("/products/list", response_model=List[ProductOut])
def list_products(payload: ProductsListRequest, db: Session = Depends(get_db)):
    business = db.query(Business).filter(Business.owner_id == payload.user_id).first()
    if not business:
        raise HTTPException(status_code=404, detail="Business not found for user")
    products = db.query(Product).filter(Product.business_id == business.id).all()
    return products







class ProductCreateIn(BaseModel):
    user_id: int
    title: str = Field(..., min_length=2, max_length=80)
    price: Annotated[Decimal, Field(gt=0, max_digits=9, decimal_places=2)]
    sku: str | None = None
    description: str | None = None
    keywords: list[str] = []
    image_url: str | None = None


class ProductOut(BaseModel):
    id: int
    title: str
    price: float
    sku: str | None = None
    description: str | None = None
    keywords: str | None = None
    image_url: str | None = None

    class Config:
        orm_mode = True



@app.post("/products/create", response_model=ProductOut)
def add_product(payload: ProductCreateIn = Body(...), db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == payload.user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    business = db.query(Business).filter(Business.owner_id == user.id).first()
    if not business:
        raise HTTPException(status_code=404, detail="Business not found for user")

    prod = Product(
        business_id = business.id,
        title       = payload.title,
        price       = float(payload.price),
        sku         = payload.sku,
        description = payload.description,
        keywords    = ",".join(payload.keywords) if payload.keywords else None,
        image_url   = payload.image_url
    )
    db.add(prod)
    db.commit()
    db.refresh(prod)
    return prod


def upload_image_to_cloudinary(file):
    try:
        result = cloudinary.uploader.upload(file.file)
        return result.get("secure_url")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Upload failed: {str(e)}")

@app.post("/upload_image")
def upload_image(file: UploadFile = File(...)):
    image_url = upload_image_to_cloudinary(file)
    return {"image_url": image_url}






class ProductIdRequest(BaseModel):
    user_id: int
    product_id: int

class ProductSkuRequest(BaseModel):
    user_id: int
    sku: str

class ProductEditByIdRequest(BaseModel):
    user_id: int
    product_id: int
    sku: str
    title: str
    price: Decimal
    description: str | None = None
    keywords: list[str] = []
    image_url: str | None = None



@app.post("/products/get_by_id")
def get_product_by_id(payload: ProductIdRequest = Body(...), db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == payload.user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    business = db.query(Business).filter(Business.owner_id == user.id).first()
    if not business:
        raise HTTPException(status_code=404, detail="Business not found for user")

    product = db.query(Product).filter(
        Product.business_id == business.id,
        Product.id == payload.product_id
    ).first()

    if not product:
        raise HTTPException(status_code=404, detail="Product not found")

    return {
        "id": product.id,
        "title": product.title,
        "price": float(product.price),
        "sku": product.sku,
        "description": product.description,
        "keywords": product.keywords.split(",") if product.keywords else [],
        "image_url": product.image_url
    }


@app.post("/products/edit_by_id")
def edit_product_by_id(payload: ProductEditByIdRequest = Body(...), db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == payload.user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    business = db.query(Business).filter(Business.owner_id == user.id).first()
    if not business:
        raise HTTPException(status_code=404, detail="Business not found for user")

    product = db.query(Product).filter(
        Product.business_id == business.id,
        Product.id == payload.product_id
    ).first()

    if not product:
        raise HTTPException(status_code=404, detail="Product not found")

    product.title = payload.title
    product.price = float(payload.price)
    product.sku = payload.sku
    product.description = payload.description
    product.keywords = ",".join(payload.keywords) if payload.keywords else None
    product.image_url = payload.image_url

    db.commit()
    db.refresh(product)

    return {
        "message": "Product updated successfully",
        "product": {
            "id": product.id,
            "title": product.title,
            "price": float(product.price),
            "sku": product.sku,
            "description": product.description,
            "keywords": product.keywords.split(",") if product.keywords else [],
            "image_url": product.image_url
        }
    }


@app.post("/products/delete_by_id")
def delete_product_by_id(payload: ProductIdRequest = Body(...), db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == payload.user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    business = db.query(Business).filter(Business.owner_id == user.id).first()
    if not business:
        raise HTTPException(status_code=404, detail="Business not found for user")

    product = db.query(Product).filter(
        Product.business_id == business.id,
        Product.id == payload.product_id
    ).first()

    if not product:
        raise HTTPException(status_code=404, detail="Product not found")

    db.delete(product)
    db.commit()

    return {
        "message": "Product deleted successfully",
        "product_id": payload.product_id
    }






class ProductScanRequest(BaseModel):
    user_id: int
    # business_id: int
    detected_code: str

@app.post("/products/scan")
def scan_product(payload: ProductScanRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == payload.user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    business = db.query(Business).filter(Business.owner_id == user.id).first()
    if not business:
        raise HTTPException(status_code=404, detail="Business not found for user")



    product = db.query(Product).filter(
        Product.business_id == business.id,
        Product.sku == payload.detected_code
    ).first()

    if not product:
        raise HTTPException(status_code=404, detail=f"No product with scanned code: {payload.detected_code}")

    return {
        "message": f"✅ Scan successful! Found product for code {payload.detected_code}",
        "product": {
            "id": product.id,
            "title": product.title,
            "price": float(product.price),
            "sku": product.sku,
            "description": product.description,
            "keywords": product.keywords.split(",") if product.keywords else [],
            "image_url": product.image_url
        }
    }




print("V2 Achieved")





class ProfilePictureStatusRequest(BaseModel):
    user_id: int

class ProfilePictureStatusResponse(BaseModel):
    has_profile_picture: bool
    image_url: str | None = None

class UploadProfilePictureResponse(BaseModel):
    image_url: str
    user_id: str

# Add this function for the midnight cleanup job
# def reset_all_profile_pictures():
#     """Reset all users' profile pictures to NULL at midnight"""
#     db = SessionLocal()
#     try:
#         # Update all users to set profile_picture_url to NULL
#         db.query(User).update({User.profile_picture_url: None})
#         db.commit()
#         print(f"✅ Profile pictures reset at {datetime.now()}")
#     except Exception as e:
#         print(f"❌ Error resetting profile pictures: {e}")
#         db.rollback()
#     finally:
#         db.close()

# # Add scheduler setup (put this after your app initialization)
# scheduler = BackgroundScheduler()
# scheduler.add_job(
#     func=reset_all_profile_pictures,
#     trigger=CronTrigger(hour=0, minute=0),  # Run at midnight every day
#     id='reset_profile_pictures',
#     name='Reset profile pictures at midnight',
#     replace_existing=True
# )
# scheduler.start()
from fastapi import Form
@app.get("/user/profile_picture_status")
def get_profile_picture_status(user_id: int, db: Session = Depends(get_db)):
    """Check if user has a valid profile picture"""
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    has_picture = user.profile_picture_url is not None and user.profile_picture_url.strip() != ""
    
    return ProfilePictureStatusResponse(
        has_profile_picture=has_picture,
        image_url=user.profile_picture_url if has_picture else None
    )

@app.post("/user/upload_profile_picture")
def upload_profile_picture(
    user_id: str = Form(...),
    image: UploadFile = File(...),
    db: Session = Depends(get_db)
):
    """Upload and save user's profile picture"""
    try:
        user_id_int = int(user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid user_id format")
    
    # Check if user exists
    user = db.query(User).filter(User.id == user_id_int).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Validate file type
    if not image.content_type.startswith('image/'):
        raise HTTPException(status_code=400, detail="File must be an image")
    
    try:
        # Upload to Cloudinary
        result = cloudinary.uploader.upload(
            image.file,
            folder="profile_pictures",
            public_id=f"user_{user_id_int}_{int(datetime.now().timestamp())}"
        )
        
        image_url = result.get("secure_url")
        
        # Save URL to database
        user.profile_picture_url = image_url
        db.commit()
        db.refresh(user)
        
        return UploadProfilePictureResponse(
            image_url=image_url,
            user_id=user_id
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Upload failed: {str(e)}")


print("Profile picture endpoints added!")

# Add these imports at the top
from enum import Enum
from typing import Dict, Optional

# Add these models after your existing BaseModel classes
class PaymentRequestStatus(str, Enum):
    PENDING = "pending"
    ACCEPTED = "accepted"
    DECLINED = "declined"

class PaymentRequestPayload(BaseModel):
    merchant_user_id: int
    customer_user_id: int
    amount: float = Field(gt=0, le=9999.99)

class PaymentResponsePayload(BaseModel):
    customer_user_id: int
    action: str  # "accept" or "decline"

# Simple in-memory storage for active requests (one per customer)
# Format: {customer_user_id: request_data}
active_requests: Dict[int, dict] = {}

# Add these endpoints after your existing ones

@app.post("/payment/request")
def create_payment_request(payload: PaymentRequestPayload, db: Session = Depends(get_db)):
    """Screen 4: Merchant creates payment request"""
    
    # Validate merchant exists
    merchant = db.query(User).filter(User.id == payload.merchant_user_id).first()
    if not merchant:
        raise HTTPException(status_code=404, detail="Merchant not found")
    
    # Validate customer exists
    customer = db.query(User).filter(User.id == payload.customer_user_id).first()
    if not customer:
        raise HTTPException(status_code=404, detail="Customer not found")
    
    # Validate amount
    amount_cents = int(round(payload.amount * 100))
    
    # Check customer balance
    if customer.balance_cents < amount_cents:
        raise HTTPException(
            status_code=400, 
            detail=f"Customer has insufficient funds. Available: ${customer.balance_cents/100:.2f}"
        )
    
    # Prevent self-payment
    if payload.merchant_user_id == payload.customer_user_id:
        raise HTTPException(status_code=400, detail="Cannot request payment from yourself")
    
    # Generate request ID
    request_id = f"req_{payload.merchant_user_id}_{payload.customer_user_id}_{int(datetime.now().timestamp())}"
    
    # Store the request (overwrite any existing request for this customer)
    request_data = {
        "request_id": request_id,
        "merchant_user_id": payload.merchant_user_id,
        "customer_user_id": payload.customer_user_id,
        "amount": payload.amount,
        "amount_cents": amount_cents,
        "status": PaymentRequestStatus.PENDING,
        "created_at": datetime.now(),
        "merchant_name": f"{merchant.first_name} {merchant.last_name}",
        "merchant_profile_url": merchant.profile_picture_url
    }
    
    active_requests[payload.customer_user_id] = request_data
    
    return {
        "success": True,
        "request_id": request_id,
        "message": f"Payment request sent to {customer.first_name} {customer.last_name}",
        "amount": payload.amount,
        "customer_name": f"{customer.first_name} {customer.last_name}"
    }

@app.post("/payment/check_request")
def check_payment_request(payload: dict, db: Session = Depends(get_db)):
    """Screen 5: Customer checks if they have a pending payment request"""
    
    user_id = payload.get("user_id")
    if not user_id:
        raise HTTPException(status_code=400, detail="user_id required")
    
    # Check if customer has a pending request
    request_data = active_requests.get(user_id)
    
    if not request_data or request_data["status"] != PaymentRequestStatus.PENDING:
        return {
            "has_pending_request": False,
            "message": "No pending payment requests"
        }
    
    return {
        "has_pending_request": True,
        "request_id": request_data["request_id"],
        "merchant_name": request_data["merchant_name"],
        "merchant_profile_url": request_data.get("merchant_profile_url"),
        "amount": request_data["amount"],
        "created_at": request_data["created_at"]
    }

@app.post("/payment/respond")
def respond_to_payment_request(payload: PaymentResponsePayload, db: Session = Depends(get_db)):
    """Screen 5: Customer accepts or declines payment request"""
    
    # Get the customer's active request
    request_data = active_requests.get(payload.customer_user_id)
    if not request_data:
        raise HTTPException(status_code=404, detail="No active payment request found")
    
    if request_data["status"] != PaymentRequestStatus.PENDING:
        raise HTTPException(status_code=400, detail="Request already processed")
    
    if payload.action == "decline":
        # Update status and keep in memory for merchant to see
        request_data["status"] = PaymentRequestStatus.DECLINED
        active_requests[payload.customer_user_id] = request_data
        
        return {
            "success": True,
            "action": "declined",
            "message": "Payment request declined"
        }
    
    elif payload.action == "accept":
        # Get users for balance transfer
        customer = db.query(User).filter(User.id == payload.customer_user_id).first()
        merchant = db.query(User).filter(User.id == request_data["merchant_user_id"]).first()
        
        if not customer or not merchant:
            raise HTTPException(status_code=404, detail="User not found")
        
        amount_cents = request_data["amount_cents"]
        
        # Final balance check
        if customer.balance_cents < amount_cents:
            raise HTTPException(status_code=400, detail="Insufficient funds")
        
        # Transfer money
        customer.balance_cents -= amount_cents
        merchant.balance_cents += amount_cents
        db.commit()
        
        # Update request status
        request_data["status"] = PaymentRequestStatus.ACCEPTED
        request_data["completed_at"] = datetime.now()
        active_requests[payload.customer_user_id] = request_data
        
        return {
            "success": True,
            "action": "accepted",
            "message": f"Payment of ${request_data['amount']:.2f} completed successfully!",
            "new_balance": customer.balance_cents / 100,
            "amount_paid": request_data["amount"]
        }
    
    else:
        raise HTTPException(status_code=400, detail="Action must be 'accept' or 'decline'")

@app.get("/payment/status/{customer_user_id}")
def get_payment_status(customer_user_id: int):
    """Screen 6: Merchant polls for payment status"""
    
    request_data = active_requests.get(customer_user_id)
    if not request_data:
        raise HTTPException(status_code=404, detail="No payment request found")
    
    # Auto-cleanup completed/declined requests after returning status
    if request_data["status"] in [PaymentRequestStatus.ACCEPTED, PaymentRequestStatus.DECLINED]:
        # Return status first, then clean up
        status_response = {
            "status": request_data["status"],
            "amount": request_data["amount"],
            "customer_name": f"Customer {customer_user_id}",  # You could get actual name if needed
            "completed": True,
            "message": f"Payment {request_data['status'].value}"
        }
        
        # Clean up the request after 1 second delay (in real app, use background task)
        # For now, we'll keep it for the response
        return status_response
    
    return {
        "status": request_data["status"],
        "amount": request_data["amount"],
        "completed": False,
        "message": "Waiting for customer response..."
    }

print("Simplified real-time payment endpoints added!")