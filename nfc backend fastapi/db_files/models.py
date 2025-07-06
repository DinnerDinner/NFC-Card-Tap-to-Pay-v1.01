import sys
import os
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from sqlalchemy import Column, Integer, String, Date, DateTime, Boolean
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
    card_uid = Column(String, unique=True, index=True, nullable=True)               # physical card
    cadet_card_uid = Column(String, unique=True, index=True, nullable=True)         # virtual cadet card
    hospital_card_uid = Column(String, unique=True, index=True, nullable=True)      # virtual hospital card
    school_card_uid = Column(String, unique=True, index=True, nullable=True)        # virtual school card

    # Use-case flags
    is_cadet = Column(Boolean, default=False)
    is_hospital_user = Column(Boolean, default=False)
    is_student = Column(Boolean, default=False)

    def __repr__(self):
        return f"<User {self.first_name} {self.last_name} | email={self.email}>"

