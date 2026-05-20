package com.commitOrShame.backend.scheduler;

import com.commitOrShame.backend.service.ShameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShameScheduler {

    private final ShameService shameService;

    // Runs every day at 11:30 PM — enough time to commit, not enough to escape
    @Scheduled(cron = "0 30 23 * * *")
    public void runDailyShameCheck() {
        log.info("ShameScheduler triggered");
        shameService.runDailyShameCheck();
    }
}