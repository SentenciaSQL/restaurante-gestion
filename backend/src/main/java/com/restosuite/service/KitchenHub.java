package com.restosuite.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class KitchenHub {

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long branchId) {
        SseEmitter emitter = new SseEmitter(30L * 60L * 1000L);
        emitters.computeIfAbsent(branchId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(branchId, emitter));
        emitter.onTimeout(() -> remove(branchId, emitter));
        emitter.onError(error -> remove(branchId, emitter));
        try {
            emitter.send(SseEmitter.event().name("ready").data("ok"));
        } catch (Exception ex) {
            remove(branchId, emitter);
        }
        return emitter;
    }

    public void publish(Long branchId) {
        for (SseEmitter emitter : emitters.getOrDefault(branchId, new CopyOnWriteArrayList<>())) {
            try {
                emitter.send(SseEmitter.event().name("kitchen").data("changed"));
            } catch (Exception ex) {
                remove(branchId, emitter);
            }
        }
    }

    @Scheduled(fixedRate = 20000)
    public void heartbeat() {
        emitters.forEach((branchId, list) -> {
            for (SseEmitter emitter : List.copyOf(list)) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception ex) {
                    remove(branchId, emitter);
                }
            }
        });
    }

    private void remove(Long branchId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(branchId);
        if (list != null) {
            list.remove(emitter);
        }
    }
}
