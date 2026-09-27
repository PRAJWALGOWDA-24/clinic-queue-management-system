// notification/NotificationRepository.java
package com.clinicqueue.notification;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationRepository extends JpaRepository<Notification, Long> {}