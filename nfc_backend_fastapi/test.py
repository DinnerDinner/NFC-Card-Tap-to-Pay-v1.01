# print("V2 Started!! WITH MPOS SYSTEM!!!")

# from fastapi import UploadFile, File
# # from cloudinary_utils import upload_image_to_cloudinary
# from typing import Annotated
# from pydantic import BaseModel, Field
# from decimal import Decimal
# import random
# from datetime import date, datetime
# from fastapi import FastAPI, Depends, HTTPException, status
# from pydantic import BaseModel, EmailStr
# from sqlalchemy.orm import Session
# from fastapi import Request
# from db_files.database import SessionLocal, engine, Base
# from db_files.models import User
# from pydantic import BaseModel, EmailStr, validator
# from datetime import date
# import re
# from db_files.models import Business, Product 


# from fastapi import APIRouter, Depends, HTTPException
# from sqlalchemy.orm import Session
# from typing import List
# from pydantic import BaseModel
# from fastapi import FastAPI, UploadFile, File, HTTPException, Depends, Body
# from pydantic import BaseModel, Field
# from sqlalchemy.orm import Session
# from decimal import Decimal
# from typing import List, Optional, Annotated
# import cloudinary
# import cloudinary.uploader
# Base.metadata.create_all(bind=engine)
# app = FastAPI()

# cloudinary.config(
#     cloud_name="dqhr9pgl0",
#     api_key="289124826863297",
#     api_secret="ZscnuDZzhq4uYAA2_qH6SIDTHkg"
# )

# def get_db():
#     db = SessionLocal()
#     try:
#         yield db
#     finally:
#         db.close()

# class BusinessSetupPayload(BaseModel):
#     user_id: int
#     business_name: str

# class BusinessSetupResponse(BaseModel):
#     business_id: int
#     business_name: str
#     owner_email: EmailStr
#     message: str

# @app.post("/business/setup", response_model=BusinessSetupResponse)
# def setup_business(payload: BusinessSetupPayload, db: Session = Depends(get_db)):
#     user = db.query(User).filter(User.id == payload.user_id).first()
#     if not user:
#         raise HTTPException(status_code=404, detail="User not found")

#     existing_business = db.query(Business).filter(Business.owner_id == user.id).first()
#     if existing_business:
#         return BusinessSetupResponse(
#             business_id=existing_business.id,
#             business_name=existing_business.business_name,
#             owner_email=user.email,
#             message="Business already exists for this user"
#         )

#     new_business = Business(
#         owner_id=user.id,
#         business_name=payload.business_name
#     )
#     db.add(new_business)
#     db.commit()
#     db.refresh(new_business)

#     return BusinessSetupResponse(
#         business_id=new_business.id,
#         business_name=new_business.business_name,
#         owner_email=user.email,
#         message="Business successfully created"
#     )


# @app.post("/business/exists")
# def check_business(payload: dict, db: Session = Depends(get_db)):
#     user_id = payload.get("user_id")
#     if not user_id:
#         raise HTTPException(status_code=400, detail="user_id required")

#     business = db.query(Business).filter(Business.owner_id == user_id).first()
#     if business:
#         return {
#             "has_business": True,
#             "business_name": business.business_name,
#             "business_id": business.id,         
#         }
#     return { "has_business": False }






# class ProductsListRequest(BaseModel):
#     user_id: int
    
# class ProductOut(BaseModel):
#     id: int
#     title: str
#     price: float
#     sku: str | None = None
#     barcode_number: str | None = None
#     description: str | None = None
#     keywords: str | None = None
#     image_url: str | None = None

#     class Config:
#         orm_mode = True

# @app.post("/products/list", response_model=List[ProductOut])
# def list_products(payload: ProductsListRequest, db: Session = Depends(get_db)):
#     user_id = payload.user_id
#     business = db.query(Business).filter(Business.owner_id == user_id).first()
#     if not business:
#         raise HTTPException(status_code=404, detail="Business not found for user")
#     products = db.query(Product).filter(Product.business_id == business.id).all()
#     return products



# class ProductCreateIn(BaseModel):
#     user_id: int
#     title: str = Field(..., min_length=2, max_length=80)
#     price: Annotated[Decimal, Field(gt=0, max_digits=9, decimal_places=2)]
#     sku: str | None = None
#     description: str | None = None
#     keywords: list[str] = []
#     image_url: str | None = None



# class ProductOut(BaseModel):
#     id: int
#     title: str
#     price: float
#     sku: str | None = None
#     description: str | None = None
#     keywords: str | None = None
#     image_url: str | None = None

#     class Config:
#         orm_mode = True

# @app.post("/products/create", response_model=ProductOut)
# def add_product(payload: ProductCreateIn = Body(...), db: Session = Depends(get_db)):
#     user = db.query(User).filter(User.id == payload.user_id).first()
#     if not user:
#         raise HTTPException(status_code=404, detail="User not found")

#     business = db.query(Business).filter(Business.owner_id == user.id).first()
#     if not business:
#         raise HTTPException(status_code=404, detail="Business not found for user")

#     prod = Product(
#         business_id = business.id,
#         title       = payload.title,
#         price       = float(payload.price),
#         sku         = payload.sku,
#         description = payload.description,
#         keywords    = ",".join(payload.keywords) if payload.keywords else None,
#         image_url   = payload.image_url
#     )
#     db.add(prod)
#     db.commit()
#     db.refresh(prod)
#     return prod

# def upload_image_to_cloudinary(file):
#     try:
#         result = cloudinary.uploader.upload(file.file)
#         return result.get("secure_url")
#     except Exception as e:
#         raise HTTPException(status_code=500, detail=f"Upload failed: {str(e)}")
    

    
# @app.post("/upload_image")
# def upload_image(file: UploadFile = File(...)):
#     try:
#         image_url = upload_image_to_cloudinary(file)
#         return {"image_url": image_url}
#     except Exception as e:
#         raise HTTPException(status_code=500, detail=str(e))


# class ProductSkuRequest(BaseModel):
#     user_id: int
#     sku: str

# class ProductEditRequest(BaseModel):
#     user_id: int
#     sku: str
#     title: str
#     price: Decimal
#     description: str | None = None
#     keywords: list[str] = []
#     image_url: str | None = None

# @app.post("/products/get_by_sku")
# def get_product_by_sku(payload: ProductSkuRequest = Body(...), db: Session = Depends(get_db)):
#     user = db.query(User).filter(User.id == payload.user_id).first()
#     if not user:
#         raise HTTPException(status_code=404, detail="User not found")

#     business = db.query(Business).filter(Business.owner_id == user.id).first()
#     if not business:
#         raise HTTPException(status_code=404, detail="Business not found for user")

#     product = db.query(Product).filter(
#         Product.business_id == business.id,
#         Product.sku == payload.sku
#     ).first()

#     if not product:
#         raise HTTPException(status_code=404, detail="Product not found")

#     return {
#         "id": product.id,
#         "title": product.title,
#         "price": float(product.price),
#         "sku": product.sku,
#         "description": product.description,
#         "keywords": product.keywords.split(",") if product.keywords else [],
#         "image_url": product.image_url
#     }

# @app.post("/products/edit")
# def edit_product(payload: ProductEditRequest = Body(...), db: Session = Depends(get_db)):
#     user = db.query(User).filter(User.id == payload.user_id).first()
#     if not user:
#         raise HTTPException(status_code=404, detail="User not found")

#     business = db.query(Business).filter(Business.owner_id == user.id).first()
#     if not business:
#         raise HTTPException(status_code=404, detail="Business not found for user")

#     product = db.query(Product).filter(
#         Product.business_id == business.id,
#         Product.sku == payload.sku
#     ).first()

#     if not product:
#         raise HTTPException(status_code=404, detail="Product not found")

#     # Update fields
#     product.title = payload.title
#     product.price = float(payload.price)
#     product.description = payload.description
#     product.keywords = ",".join(payload.keywords) if payload.keywords else None
#     product.image_url = payload.image_url

#     db.commit()
#     db.refresh(product)

#     return {
#         "message": "Product updated successfully",
#         "product": {
#             "id": product.id,
#             "title": product.title,
#             "price": float(product.price),
#             "sku": product.sku,
#             "description": product.description,
#             "keywords": product.keywords.split(",") if product.keywords else [],
#             "image_url": product.image_url
#         }
#     }

# @app.post("/products/delete")
# def delete_product(payload: ProductSkuRequest = Body(...), db: Session = Depends(get_db)):
#     user = db.query(User).filter(User.id == payload.user_id).first()
#     if not user:
#         raise HTTPException(status_code=404, detail="User not found")

#     business = db.query(Business).filter(Business.owner_id == user.id).first()
#     if not business:
#         raise HTTPException(status_code=404, detail="Business not found for user")

#     product = db.query(Product).filter(
#         Product.business_id == business.id,
#         Product.sku == payload.sku
#     ).first()

#     if not product:
#         raise HTTPException(status_code=404, detail="Product not found")

#     db.delete(product)
#     db.commit()

#     return {
#         "message": "Product deleted successfully",
#         "sku": payload.sku
#     }

