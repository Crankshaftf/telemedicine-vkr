from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.models import Patient, User, UserRole, UserStatus
from app.schemas import LoginRequest, ProfileResponse, ProfileUpdateRequest, RegisterRequest, TokenResponse, UserResponse
from app.utils.deps import get_current_user
from app.utils.security import create_access_token, hash_password, verify_password

router = APIRouter(prefix="/auth", tags=["Авторизация"])


def _normalize_login(login: str) -> str:
    return login.strip().lower()


@router.post("/register", response_model=TokenResponse, status_code=status.HTTP_201_CREATED)
def register(data: RegisterRequest, db: Session = Depends(get_db)):
    if not data.consent:
        raise HTTPException(status_code=400, detail="Необходимо согласие на обработку персональных данных")

    login = _normalize_login(data.email)
    if db.query(User).filter(User.login == login).first():
        raise HTTPException(status_code=400, detail="Пользователь с таким email уже существует")

    user = User(
        login=login,
        password_hash=hash_password(data.password),
        role=UserRole.PATIENT,
        status=UserStatus.ACTIVE,
    )
    db.add(user)
    db.flush()

    patient = Patient(
        user_id=user.user_id,
        full_name=data.full_name,
        phone=data.phone,
        email=str(data.email),
    )
    db.add(patient)
    db.commit()

    token = create_access_token(user.user_id, user.role.value)
    return TokenResponse(access_token=token)


@router.post("/login", response_model=TokenResponse)
def login(data: LoginRequest, db: Session = Depends(get_db)):
    login_value = _normalize_login(data.login)
    user = db.query(User).filter(User.login == login_value).first()

    if user is None:
        patient = db.query(Patient).filter(Patient.phone == data.login.strip()).first()
        if patient:
            user = patient.user

    if user is None or not verify_password(data.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Неверный логин или пароль")
    if user.status != UserStatus.ACTIVE:
        raise HTTPException(status_code=403, detail="Аккаунт заблокирован")

    token = create_access_token(user.user_id, user.role.value)
    return TokenResponse(access_token=token)


@router.post("/logout")
def logout():
    return {"message": "Выход выполнен. Удалите токен на клиенте."}


profile_router = APIRouter(prefix="/profile", tags=["Профиль"])


@profile_router.get("", response_model=ProfileResponse)
def get_profile(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if user.role == UserRole.PATIENT and user.patient:
        p = user.patient
        return ProfileResponse(
            user_id=user.user_id,
            full_name=p.full_name,
            birth_date=p.birth_date.isoformat() if p.birth_date else None,
            phone=p.phone,
            email=p.email,
            gender=p.gender,
            address=p.address,
            notify_appointments=p.notify_appointments,
            notify_results=p.notify_results,
            role=user.role.value,
        )
    if user.role == UserRole.DOCTOR and user.doctor:
        d = user.doctor
        return ProfileResponse(
            user_id=user.user_id,
            full_name=d.full_name,
            phone="",
            email=user.login,
            role=user.role.value,
        )
    if user.role in (UserRole.ADMIN, UserRole.MANAGER, UserRole.IT):
        role_names = {
            UserRole.ADMIN: "Администратор",
            UserRole.MANAGER: "Менеджер",
            UserRole.IT: "IT-специалист",
        }
        return ProfileResponse(
            user_id=user.user_id,
            full_name=role_names.get(user.role, user.role.value),
            phone="",
            email=user.login,
            role=user.role.value,
        )
    raise HTTPException(status_code=404, detail="Профиль не найден")


@profile_router.put("", response_model=ProfileResponse)
def update_profile(
    data: ProfileUpdateRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if user.role != UserRole.PATIENT or not user.patient:
        raise HTTPException(status_code=403, detail="Редактирование доступно только пациентам")

    p = user.patient
    if data.full_name is not None:
        p.full_name = data.full_name
    if data.birth_date is not None:
        from datetime import date
        p.birth_date = date.fromisoformat(data.birth_date) if data.birth_date else None
    if data.phone is not None:
        p.phone = data.phone
    if data.email is not None:
        p.email = str(data.email)
        user.login = _normalize_login(str(data.email))
    if data.gender is not None:
        p.gender = data.gender
    if data.address is not None:
        p.address = data.address
    if data.notify_appointments is not None:
        p.notify_appointments = data.notify_appointments
    if data.notify_results is not None:
        p.notify_results = data.notify_results

    db.commit()
    db.refresh(p)
    return get_profile(user, db)
