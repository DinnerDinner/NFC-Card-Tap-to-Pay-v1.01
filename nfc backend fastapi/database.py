from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base
from sqlalchemy import create_engine, text

# Update this with your Docker PostgreSQL connection details
DATABASE_URL = "postgresql://nfcuser:nfcpass@localhost:5432/nfcwallet"

# Create SQLAlchemy engine
engine = create_engine(DATABASE_URL)

# Create a configured "Session" class
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Base class for models
Base = declarative_base()


#TESTING!
if __name__ == "__main__":
    try:
        with engine.connect() as conn:
            result = conn.execute(text("SELECT 1"))
            print("✅ Database connected, test query result:", result.scalar())
    except Exception as e:
        print("❌ Database connection failed:", e)