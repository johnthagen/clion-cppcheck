package com.github.johnthagen.cppcheck;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import org.jetbrains.annotations.NotNull;

class CppcheckNotification {
    public static void send(@NotNull final String title, @NotNull final String content, final NotificationType type) {
        Notifications.Bus.notify(new Notification("Cppcheck",
                "Cppcheck " + title,
                content,
                type));
    }
}
