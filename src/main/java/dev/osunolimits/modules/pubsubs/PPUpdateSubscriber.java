package dev.osunolimits.modules.pubsubs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import dev.osunolimits.main.App;
import dev.osunolimits.modules.CacheInvalidator;
import dev.osunolimits.modules.pubsubs.PubSubModels.PPUpdateMessage;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class PPUpdateSubscriber {
    private static final Logger log = LoggerFactory.getLogger("PPUpdateSubscriber");
    private static final Gson gson = new Gson();
    private static final String CHANNEL = "shiina:pp_update";

    public static void start() {
        Thread subscriberThread = new Thread(() -> {
            while (true) {
                try {
                    subscribeToChannel();
                } catch (Exception e) {
                    log.error("PubSub connection error, reconnecting in 5 seconds...", e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.info("PubSub subscriber thread interrupted, stopping.");
                        break;
                    }
                }
            }
        });
        subscriberThread.setName("PPUpdateSubscriber");
        subscriberThread.setDaemon(true);
        subscriberThread.start();
        log.info("Started PP Update subscriber on channel: {}", CHANNEL);
    }

    private static void subscribeToChannel() {
        HostAndPort hostAndPort = new HostAndPort(
            App.env.get("REDISHOST"),
            Integer.parseInt(App.env.get("REDISPORT"))
        );

        DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
            .connectionTimeoutMillis(Integer.parseInt(App.env.get("REDISTIMEOUT")))
            .database(Integer.parseInt(App.env.get("REDISDB")))
            .password(App.env.get("REDISPASS"))
            .user(App.env.get("REDISUSER"))
            .build();

        try (Jedis jedis = new Jedis(hostAndPort, clientConfig)) {
            jedis.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    handlePPUpdate(message);
                }

                @Override
                public void onSubscribe(String channel, int subscribedChannels) {
                    log.info("Subscribed to channel: {}", channel);
                }

                @Override
                public void onUnsubscribe(String channel, int subscribedChannels) {
                    log.info("Unsubscribed from channel: {}", channel);
                }
            }, CHANNEL);
        }
    }

    private static void handlePPUpdate(String message) {
        try {
            PPUpdateMessage ppUpdate = gson.fromJson(message, PPUpdateMessage.class);
            log.debug("Received PP update: user_id={}, mode={}, pp={}",
                ppUpdate.user_id, ppUpdate.mode, ppUpdate.pp);

            // Clear API caches to force fresh data on next request
            CacheInvalidator.clearAllApiCaches();

            log.debug("Cache invalidated for PP update");
        } catch (Exception e) {
            log.error("Failed to process PP update message: {}", message, e);
        }
    }
}
