print("V2 Started!! WITH MPOS SYSTEM!!!")

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
from db_files.database import SessionLocal, engine, Base
from db_files.models import User
from pydantic import BaseModel, EmailStr, validator
from datetime import date
import re
from db_files.models import Business, Product 


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
    try:
        image_url = upload_image_to_cloudinary(file)
        return {"image_url": image_url}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


class ProductSkuRequest(BaseModel):
    user_id: int
    sku: str

class ProductEditRequest(BaseModel):
    user_id: int
    sku: str
    title: str
    price: Decimal
    description: str | None = None
    keywords: list[str] = []
    image_url: str | None = None





####



@app.post("/products/get_by_sku")
def get_product_by_sku(payload: ProductSkuRequest = Body(...), db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == payload.user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    business = db.query(Business).filter(Business.owner_id == user.id).first()
    if not business:
        raise HTTPException(status_code=404, detail="Business not found for user")

    product = db.query(Product).filter(
        Product.business_id == business.id,
        Product.sku == payload.sku
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





@app.post("/products/edit")
def edit_product(payload: ProductEditRequest = Body(...), db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == payload.user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    business = db.query(Business).filter(Business.owner_id == user.id).first()
    if not business:
        raise HTTPException(status_code=404, detail="Business not found for user")

    product = db.query(Product).filter(
        Product.business_id == business.id,
        Product.sku == payload.sku
    ).first()

    if not product:
        raise HTTPException(status_code=404, detail="Product not found")

    # Update fields
    product.title = payload.title
    product.price = float(payload.price)
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




@app.post("/products/delete")
def delete_product(payload: ProductSkuRequest = Body(...), db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == payload.user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    business = db.query(Business).filter(Business.owner_id == user.id).first()
    if not business:
        raise HTTPException(status_code=404, detail="Business not found for user")

    product = db.query(Product).filter(
        Product.business_id == business.id,
        Product.sku == payload.sku
    ).first()

    if not product:
        raise HTTPException(status_code=404, detail="Product not found")

    db.delete(product)
    db.commit()

    return {
        "message": "Product deleted successfully",
        "sku": payload.sku
    }




