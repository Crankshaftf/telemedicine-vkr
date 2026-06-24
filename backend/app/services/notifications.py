from sqlalchemy.orm import Session

from app.models import Notification, NotificationStatus


def create_notification(db: Session, user_id: int, type_: str, text: str) -> Notification:
    notification = Notification(user_id=user_id, type=type_, text=text, status=NotificationStatus.UNREAD)
    db.add(notification)
    return notification
