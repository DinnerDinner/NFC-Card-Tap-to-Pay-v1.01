from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base
from sqlalchemy import create_engine, text

# Update this with your Docker PostgreSQL connection details
SQLALCHEMY_DATABASE_URL = "postgresql://postgres.fjqdbfspwnjhbimgasrg:DinnerDinner2024@aws-0-us-east-2.pooler.supabase.com:5432/postgres"

engine = create_engine(SQLALCHEMY_DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


#TESTING!
if __name__ == "__main__":
    try:
        with engine.connect() as conn:
            result = conn.execute(text("SELECT 1"))
            print("✅ Database connected, test query result:", result.scalar())
    except Exception as e:
        print("❌ Database connection failed:", e)