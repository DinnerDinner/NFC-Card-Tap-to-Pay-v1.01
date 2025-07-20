print("Loading models.py")

from sqlalchemy import Column, Integer, String, Date, DateTime, Boolean, ForeignKey, Float
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from db_files.database import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)

    # Identity Info
    first_name = Column(String, nullable=False)
    last_name = Column(String, nullable=False)
    email = Column(String, unique=True, index=True, nullable=False)
    phone_number = Column(String, unique=True, index=True, nullable=False)
    dob = Column(Date, nullable=False)
    password_hash = Column(String, nullable=False)

    # System & Meta
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    last_login = Column(DateTime(timezone=True), nullable=True)
    status = Column(String, default="active")  # e.g., active / suspended / deleted

    # Balance
    balance_cents = Column(Integer, default=0)

    # NFC UID Fields
    card_uid = Column(String, unique=True, index=True, nullable=True)
    cadet_card_uid = Column(String, unique=True, index=True, nullable=True)
    hospital_card_uid = Column(String, unique=True, index=True, nullable=True)
    school_card_uid = Column(String, unique=True, index=True, nullable=True)

    # Use-case flags
    is_cadet = Column(Boolean, default=False)
    is_hospital_user = Column(Boolean, default=False)
    is_student = Column(Boolean, default=False)

    # Relationships
    business = relationship("Business", back_populates="owner", uselist=False)

    def __repr__(self):
        return f"<User {self.first_name} {self.last_name} | email={self.email}>"

class Business(Base):
    __tablename__ = "businesses"

    id = Column(Integer, primary_key=True, index=True)
    owner_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    business_name = Column(String, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationships
    owner = relationship("User", back_populates="business")
    products = relationship("Product", back_populates="business", cascade="all, delete-orphan")

    def __repr__(self):
        return f"<Business {self.business_name} | owner_id={self.owner_id}>"

class Product(Base):
    __tablename__ = "products"

    id = Column(Integer, primary_key=True, index=True)
    business_id = Column(Integer, ForeignKey("businesses.id"), nullable=False)

    # Product Info
    title = Column(String, nullable=False)
    price = Column(Float, nullable=False)
    sku = Column(String, unique=True, index=True, nullable=True)
    barcode_number = Column(String, unique=True, index=True, nullable=True)
    description = Column(String, nullable=True)
    keywords = Column(String, nullable=True)  # You can parse this into a list in Python
    image_url = Column(String, nullable=True)  # placeholder or image hosting link

    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationship
    business = relationship("Business", back_populates="products")

    def __repr__(self):
        return f"<Product {self.title} | business_id={self.business_id}>"
