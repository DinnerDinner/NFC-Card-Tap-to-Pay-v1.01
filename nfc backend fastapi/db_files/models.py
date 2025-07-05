import sys
import os
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from sqlalchemy import Column, Integer, String, Float, DateTime
from sqlalchemy.sql import func
from database import Base

class Card(Base):
    __tablename__ = "cards"

    id         = Column(Integer, primary_key=True, index=True)
    uid        = Column(String, unique=True, index=True, nullable=False)
    balance    = Column(Float, default=0.0)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())

    def __repr__(self):
        return f"<Card uid={self.uid} balance={self.balance}>"
