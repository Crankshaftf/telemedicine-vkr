from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.models import Notification, NotificationStatus, User
from app.schemas import NotificationResponse
from app.utils.deps import get_current_user

router = APIRouter(prefix="/notifications", tags=["Уведомления"])


@router.get("", response_model=list[NotificationResponse])
def get_notifications(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return (
        db.query(Notification)
        .filter(Notification.user_id == user.user_id)
        .order_by(Notification.created_at.desc())
        .all()
    )


@router.put("/{notification_id}/read", response_model=NotificationResponse)
def mark_as_read(
    notification_id: int,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    notification = db.get(Notification, notification_id)
    if notification is None or notification.user_id != user.user_id:
        raise HTTPException(status_code=404, detail="Уведомление не найдено")

    notification.status = NotificationStatus.READ
    db.commit()
    db.refresh(notification)
    return notification
