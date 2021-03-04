package com.microservices.demo.twitter.to.kafka.service.runner.impl;

import com.microservices.demo.twitter.to.kafka.service.config.TwitterToKafkaServiceConfig;
import com.microservices.demo.twitter.to.kafka.service.listener.TwitterKafkaStatusListener;
import com.microservices.demo.twitter.to.kafka.service.runner.StreamRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnProperty(name = "twitter-to-kafka-service.enable-mock-tweets", havingValue = "true")
public class MockKafkaStreamRunner implements StreamRunner {
    private static final Random RANDOM = new Random();

    private static final String[] WORDS = new String[]{
            "Lorem",
            "ipsum",
            "dolor",
            "sit",
            "amet",
            "consectetuer",
            "adipiscing",
            "elit",
            "Maecenas",
            "porttitor",
            "congue",
            "massa",
            "Fusce",
            "posuere",
            "magna",
            "sed",
            "pulvinar",
            "ultricies",
            "purus",
            "lectus",
            "malesuada",
            "libero"
    };

    private static final String tweetAsRawJson = "{" +
            "\"created_at\":\"{0}\"," +
            "\"id\":\"{1}\"," +
            "\"text\":\"{2}\"," +
            "\"user\":{\"id\":\"{3}\"}" +
            "}";

    private static final String TWITTER_STATUS_DATE_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";

    private final TwitterToKafkaServiceConfig twitterToKafkaServiceConfig;
    private final TwitterKafkaStatusListener twitterKafkaStatusListener;

    public MockKafkaStreamRunner(TwitterToKafkaServiceConfig twitterToKafkaServiceConfig, TwitterKafkaStatusListener twitterKafkaStatusListener) {
        this.twitterToKafkaServiceConfig = twitterToKafkaServiceConfig;
        this.twitterKafkaStatusListener = twitterKafkaStatusListener;
    }

    @Override
    public void start() throws TwitterException {
        final List<String> keywords = twitterToKafkaServiceConfig.getTwitterKeywords();
        final int minTweetLength = twitterToKafkaServiceConfig.getMockMinTweetLength();
        final int maxTweetLength = twitterToKafkaServiceConfig.getMockMaxTweetLength();
        final long sleepTimeMs = twitterToKafkaServiceConfig.getMockSleepMs();

        log.info("Starting mock filtering twitter streams for keywords {}", keywords);
        simulateTwitterStream(keywords, minTweetLength, maxTweetLength, sleepTimeMs);
    }

    private void simulateTwitterStream(List<String> keywords, int minTweetLength, int maxTweetLength, long sleepTimeMs) {
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(()-> {
                    try {
                        String formattedTweetAsRawJson = getFormattedTweet(keywords, minTweetLength, maxTweetLength);
                        Status status = TwitterObjectFactory.createStatus(formattedTweetAsRawJson);
                        twitterKafkaStatusListener.onStatus(status);
                    } catch (TwitterException e) {
                        log.error("Error creating twitter status!", e);
                    }
                },
                0,
                sleepTimeMs,
                TimeUnit.MILLISECONDS
        );
    }

    private String getFormattedTweet(final List<String> keywords, final int minTweetLength, final int maxTweetLength) {
        List<String> params = List.of(
                ZonedDateTime.now().format(DateTimeFormatter.ofPattern(TWITTER_STATUS_DATE_FORMAT, Locale.ENGLISH)),
                String.valueOf(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE)),
                getRandomTweetContent(keywords, minTweetLength, maxTweetLength),
                String.valueOf(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE))
        );
        return formatTweetAsJsonWithParams(params);
    }

    private String formatTweetAsJsonWithParams(final List<String> params) {
        String tweet = tweetAsRawJson;
        for (int i = 0; i < params.size(); i++) {
            tweet = tweet.replace("{" + i + "}", params.get(i));
        }
        return tweet;
    }

    private String getRandomTweetContent(List<String> keywords, int minTweetLength, int maxTweetLength) {
        StringBuilder tweet = new StringBuilder();
        int tweetLength = RANDOM.nextInt(maxTweetLength - minTweetLength + 1) + minTweetLength;
        return constructRandomTweet(keywords, tweet, tweetLength);
    }

    private String constructRandomTweet(List<String> keywords, StringBuilder tweet, int tweetLength) {
        for (int i = 0; i < tweetLength; i++) {
            tweet.append(WORDS[RANDOM.nextInt(WORDS.length)]).append(" ");
            if (i == tweetLength / 2) {
                tweet.append(keywords.get(RANDOM.nextInt(keywords.size()))).append(" ");
            }
        }
        return tweet.toString().trim();
    }
}
