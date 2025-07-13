import sys
import os
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from db_files.database import SessionLocal
from db_files.models import User

# USER CHECK DATABASE
def print_all_users_with_card_uid():
    session = SessionLocal()
    try:
        users = session.query(User).filter(User.card_uid != None).all()
        
        if not users:
            print("No users with card_uid found in database.")
            return
        
        for user in users:
            print(f"ID: {user.id} | Name: {user.first_name} {user.last_name} | "
                  f"Email: {user.email} | Phone: {user.phone_number} | DOB: {user.dob} | "
                  f"Card UID: {user.card_uid} | Balance (cents): {user.balance_cents} | "
                  f"Created at: {user.created_at} | Last login: {user.last_login} | "
                  f"Is Cadet: {user.is_cadet} | Is Hospital User: {user.is_hospital_user} | Is Student: {user.is_student} | "
                  f"Status: {user.status} | Password: {user.password_hash}")
    finally:
        session.close()

if __name__ == "__main__":
    print_all_users_with_card_uid()










# PRODUCTS CHECK DATABASE
import sys

from db_files.database import SessionLocal
from db_files.models import Business, Product

def print_all_businesses_and_products():
    session = SessionLocal()
    try:
        businesses = session.query(Business).all()

        if not businesses:
            print("No businesses found in database.")
            return

        for biz in businesses:
            print(f"\n=== Business ID: {biz.id} | Name: {biz.business_name} | Owner User ID: {biz.owner_id} ===")
            
            if not biz.products:
                print("  No products for this business.")
                continue

            for prod in biz.products:
                print(
                    f"  - Product ID: {prod.id}\n"
                    f"    Title: {prod.title}\n"
                    f"    Price: ${prod.price:.2f}\n"
                    f"    SKU: {prod.sku}\n"
                    f"    Barcode: {prod.barcode_number}\n"
                    f"    Description: {prod.description}\n"
                    f"    Keywords: {prod.keywords}\n"
                    f"    Created at: {prod.created_at}\n"
                )
    finally:
        session.close()

if __name__ == "__main__":
    print_all_businesses_and_products()















from db_files.database import SessionLocal
from db_files.models import Business

def print_all_businesses():
    session = SessionLocal()
    try:
        businesses = session.query(Business).all()
        if not businesses:
            print("No businesses found in database.")
            return
        for biz in businesses:
            print(f"Business ID: {biz.id} | Name: {biz.business_name} | Owner User ID: {biz.owner_id} | Created at: {biz.created_at}|")
    finally:
        session.close()

if __name__ == "__main__":
    print_all_businesses()
