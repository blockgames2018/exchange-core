package org.openpredict.exchange.tests.util;

import com.google.common.collect.Sets;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.nustaq.serialization.FSTConfiguration;
import org.openpredict.exchange.beans.CoreSymbolSpecification;
import org.openpredict.exchange.beans.L2MarketData;
import org.openpredict.exchange.beans.SymbolType;
import org.openpredict.exchange.beans.api.*;
import org.openpredict.exchange.beans.cmd.CommandResultCode;
import org.openpredict.exchange.beans.cmd.OrderCommand;
import org.openpredict.exchange.beans.cmd.OrderCommandType;
import org.openpredict.exchange.core.ExchangeApi;
import org.openpredict.exchange.core.ExchangeCore;
import org.openpredict.exchange.core.journalling.DiskSerializationProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.openpredict.exchange.core.ExchangeCore.DisruptorWaitStrategy.BUSY_SPIN;
import static org.openpredict.exchange.core.Utils.ThreadAffityMode.THREAD_AFFINITY_ENABLE_PER_LOGICAL_CORE;

@Slf4j
public class ExchangeTestContainer {

    static final int RING_BUFFER_SIZE_DEFAULT = 64 * 1024;
    static final int RISK_ENGINES_ONE = 1;
    static final int MATCHING_ENGINES_ONE = 1;
    static final int MGS_IN_GROUP_LIMIT_DEFAULT = 128;

    static final int SYMBOL_MARGIN = 5991;
    static final int SYMBOL_EXCHANGE = 9269;

    static final int UID_1 = 1442412;
    static final int UID_2 = 1442413;

    static final int SYMBOL_AUTOGENERATED_RANGE_START = 40000;

    static final int CURRENECY_USD = 840;
    static final int CURRENECY_EUR = 978;

    static final int CURRENECY_XBT = 3762;
    static final int CURRENECY_ETH = 3928;
    static final int CURRENECY_LTC = 4141;
    static final int CURRENECY_XDG = 4142;
    static final int CURRENECY_GRC = 4143;
    static final int CURRENECY_XPM = 4144;
    static final int CURRENECY_XRP = 4145;
    static final int CURRENECY_DASH = 4146;
    static final int CURRENECY_XMR = 4147;
    static final int CURRENECY_XLM = 4148;
    static final int CURRENECY_ETC = 4149;
    static final int CURRENECY_ZEC = 4150;


    public static final Set<Integer> CURRENCIES_FUTURES = Sets.newHashSet(
            CURRENECY_USD,
            CURRENECY_EUR);

    public static final Set<Integer> CURRENCIES_EXCHANGE = Sets.newHashSet(
            CURRENECY_ETH,
            CURRENECY_XBT);


    public static final Set<Integer> ALL_CURRENCIES = Sets.newHashSet(
            CURRENECY_USD,
            CURRENECY_EUR,
            CURRENECY_XBT,
            CURRENECY_LTC,
            CURRENECY_XDG,
            CURRENECY_GRC,
            CURRENECY_XPM,
            CURRENECY_XRP,
            CURRENECY_DASH,
            CURRENECY_XMR,
            CURRENECY_XLM,
            CURRENECY_ETC,
            CURRENECY_ZEC);


    public final ExchangeCore exchangeCore;
    public final ExchangeApi api;

    @Setter
    private Consumer<OrderCommand> consumer = cmd -> {
    };

    static final Consumer<OrderCommand> CHECK_SUCCESS = cmd -> assertEquals(CommandResultCode.SUCCESS, cmd.resultCode);

    public ExchangeTestContainer() {
        this(RING_BUFFER_SIZE_DEFAULT, MATCHING_ENGINES_ONE, RISK_ENGINES_ONE, MGS_IN_GROUP_LIMIT_DEFAULT, null);
    }

    public ExchangeTestContainer(final int bufferSize,
                                 final int matchingEnginesNum,
                                 final int riskEnginesNum,
                                 final int msgsInGroupLimit,
                                 final Long stateId) {

        this.exchangeCore = ExchangeCore.builder()
                .resultsConsumer(cmd -> consumer.accept(cmd))
                .serializationProcessor(new DiskSerializationProcessor("./dumps"))
                .ringBufferSize(bufferSize)
                .matchingEnginesNum(matchingEnginesNum)
                .riskEnginesNum(riskEnginesNum)
                .msgsInGroupLimit(msgsInGroupLimit)
                .threadAffityMode(THREAD_AFFINITY_ENABLE_PER_LOGICAL_CORE)
                .waitStrategy(BUSY_SPIN)
                .loadStateId(stateId) // Loading from persisted state
                .build();

        this.exchangeCore.startup();
        api = this.exchangeCore.getApi();
    }

//    public ExchangeTestContainer(final ExchangeCore exchangeCore) {
//
//        this.exchangeCore = exchangeCore;
//        this.exchangeCore.startup();
//        api = this.exchangeCore.getApi();
//    }


    public void initBasicSymbols() {

        final CoreSymbolSpecification symbol1 = CoreSymbolSpecification.builder()
                .symbolId(SYMBOL_MARGIN)
                .type(SymbolType.FUTURES_CONTRACT)
                .baseCurrency(CURRENECY_EUR)
                .quoteCurrency(CURRENECY_USD)
                .baseScaleK(1)
                .quoteScaleK(1)
                .depositBuy(2200)
                .depositSell(3210)
                .takerFee(0)
                .makerFee(0)
                .build();

        addSymbol(symbol1);

        final CoreSymbolSpecification symbol2 = CoreSymbolSpecification.builder()
                .symbolId(SYMBOL_EXCHANGE)
                .type(SymbolType.CURRENCY_EXCHANGE_PAIR)
                .baseCurrency(CURRENECY_ETH)
                .quoteCurrency(CURRENECY_XBT)
                .baseScaleK(1)
                .quoteScaleK(1)
                .takerFee(0)
                .makerFee(0)
                .build();

        addSymbol(symbol2);
    }

    public void initBasicUsers() throws InterruptedException {

        List<ApiCommand> cmds = new ArrayList<>();

        cmds.add(ApiAddUser.builder().uid(UID_1).build());
        cmds.add(ApiAdjustUserBalance.builder().uid(UID_1).transactionId(1L).amount(10_000_00L).currency(CURRENECY_USD).build());
        cmds.add(ApiAdjustUserBalance.builder().uid(UID_1).transactionId(2L).amount(1_0000_0000L).currency(CURRENECY_XBT).build());
        cmds.add(ApiAdjustUserBalance.builder().uid(UID_1).transactionId(3L).amount(1_0000_0000L).currency(CURRENECY_ETH).build());

        cmds.add(ApiAddUser.builder().uid(UID_2).build());
        cmds.add(ApiAdjustUserBalance.builder().uid(UID_2).transactionId(1L).amount(20_000_00L).currency(CURRENECY_USD).build());
        cmds.add(ApiAdjustUserBalance.builder().uid(UID_2).transactionId(2L).amount(1_0000_0000L).currency(CURRENECY_XBT).build());
        cmds.add(ApiAdjustUserBalance.builder().uid(UID_2).transactionId(3L).amount(1_0000_0000L).currency(CURRENECY_ETH).build());

        submitCommandsSync(cmds);
    }


    public void addSymbol(CoreSymbolSpecification symbol) {
        final FSTConfiguration minBin = FSTConfiguration.createMinBinConfiguration();
        minBin.registerCrossPlatformClassMappingUseSimpleName(CoreSymbolSpecification.class);
        //new MBPrinter().printMessage(minBin.asByteArray(symbol));

        final ApiBinaryDataCommand binaryCmd = ApiBinaryDataCommand.builder().transferId(0).data(symbol).build();
        submitMultiCommandSync(binaryCmd);
    }

    public void usersInit(int numUsers, Set<Integer> currencies) throws InterruptedException {

        int totalCommands = numUsers * (1 + currencies.size());
        final CountDownLatch usersLatch = new CountDownLatch(totalCommands);
        consumer = cmd -> {
            if (cmd.resultCode == CommandResultCode.SUCCESS
                    && (cmd.command == OrderCommandType.ADD_USER || cmd.command == OrderCommandType.BALANCE_ADJUSTMENT)) {
                usersLatch.countDown();
            } else {
                throw new IllegalStateException("Unexpected command");
            }

        };

        LongAdder c = new LongAdder();

        LongStream.rangeClosed(1, numUsers)
                .forEach(uid -> {
                    api.submitCommand(ApiAddUser.builder().uid(uid).build());
                    c.increment();
                    currencies.forEach(currency -> {
                        int transactionId = currency;
                        api.submitCommand(ApiAdjustUserBalance.builder().uid(uid).transactionId(transactionId).amount(10_0000_0000L).currency(currency).build());
                        c.increment();
                    });
                    if (uid % 1000000 == 0) {
                        log.debug("uid: {} usersLatch: {}", uid, usersLatch.getCount());
                    }
                });
        usersLatch.await();

        log.debug("commands sent: {} totalCommands:{}", c, totalCommands);

    }

    public void resetExchangeCore() throws InterruptedException {
        submitCommandSync(ApiReset.builder().build(), CHECK_SUCCESS);
    }

    public void submitCommandSync(ApiCommand apiCommand, Consumer<OrderCommand> validator) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        consumer = cmd -> {
            validator.accept(cmd);
            latch.countDown();
        };
        api.submitCommand(apiCommand);
        latch.await();
        consumer = cmd -> {
        };
    }

    public <T> T submitCommandSync(ApiCommand apiCommand, Function<OrderCommand, T> resultBuilder) throws InterruptedException {
        final CompletableFuture<T> future = new CompletableFuture<>();
        consumer = cmd -> future.complete(resultBuilder.apply(cmd));
        api.submitCommand(apiCommand);
        try {
            return future.get();
        } catch (ExecutionException ex) {
            throw new IllegalStateException(ex);
        } finally {
            consumer = cmd -> {
            };
        }
    }

    public void submitMultiCommandSync(ApiCommand dataCommand) {
        final CountDownLatch latch = new CountDownLatch(1);
        consumer = cmd -> {
            if (cmd.command != OrderCommandType.BINARY_DATA
                    && cmd.command != OrderCommandType.PERSIST_STATE_RISK
                    && cmd.command != OrderCommandType.PERSIST_STATE_MATCHING) {
                throw new IllegalStateException("Unexpected command");
            }
            if (cmd.resultCode == CommandResultCode.SUCCESS) {
                latch.countDown();
            } else if (cmd.resultCode != CommandResultCode.ACCEPTED) {
                throw new IllegalStateException("Unexpected result code");
            }
        };
        api.submitCommand(dataCommand);
        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
        consumer = cmd -> {
        };
    }


    void submitCommandsSync(List<ApiCommand> apiCommand) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(apiCommand.size());
        consumer = cmd -> {
            assertEquals(CommandResultCode.SUCCESS, cmd.resultCode);
            latch.countDown();
        };
        apiCommand.forEach(api::submitCommand);
        latch.await();
        consumer = cmd -> {
        };
    }


    public L2MarketData requestCurrentOrderBook(final int symbol) {
        BlockingQueue<OrderCommand> queue = attachNewConsumerQueue();
        api.submitCommand(ApiOrderBookRequest.builder().symbol(symbol).size(-1).build());
        OrderCommand orderBookCmd = waitForOrderCommands(queue, 1).get(0);
        L2MarketData actualState = orderBookCmd.marketData;
        assertNotNull(actualState);
        return actualState;
    }

    BlockingQueue<OrderCommand> attachNewConsumerQueue() {
        final BlockingQueue<OrderCommand> results = new LinkedBlockingQueue<>();
        consumer = cmd -> results.add(cmd.copy());
        return results;
    }

    List<OrderCommand> waitForOrderCommands(BlockingQueue<OrderCommand> results, int c) {
        return Stream.generate(() -> {
            try {
                return results.poll(10000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                throw new IllegalStateException();
            }
        })
                .limit(c)
                .collect(Collectors.toList());
    }


    public List<CoreSymbolSpecification> generateAndAddSymbols(final int num,
                                                               final Set<Integer> currenciesAllowed,
                                                               final AllowedSymbolTypes allowedSymbolTypes) {
        final Random random = new Random(1L);

        final Supplier<SymbolType> symbolTypeSupplier;

        switch (allowedSymbolTypes) {
            case FUTURES_CONTRACT:
                symbolTypeSupplier = () -> SymbolType.FUTURES_CONTRACT;
                break;

            case CURRENCY_EXCHANGE_PAIR:
                symbolTypeSupplier = () -> SymbolType.CURRENCY_EXCHANGE_PAIR;
                break;

            case BOTH:
            default:
                symbolTypeSupplier = () -> random.nextBoolean() ? SymbolType.FUTURES_CONTRACT : SymbolType.CURRENCY_EXCHANGE_PAIR;
                break;
        }

        final List<Integer> currencies = new ArrayList<>(currenciesAllowed);
        final List<CoreSymbolSpecification> result = new ArrayList<>();
        for (int i = 0; i < num; ) {
            int baseCurrency = currencies.get(random.nextInt(currencies.size()));
            int quoteCurrency = currencies.get(random.nextInt(currencies.size()));
            if (baseCurrency != quoteCurrency) {
                final CoreSymbolSpecification symbol = CoreSymbolSpecification.builder()
                        .symbolId(SYMBOL_AUTOGENERATED_RANGE_START + i)
                        .type(symbolTypeSupplier.get())
                        .baseCurrency(baseCurrency) // TODO for futures can be any value
                        .quoteCurrency(quoteCurrency)
                        .baseScaleK(1)
                        .quoteScaleK(1)
                        .takerFee(0)
                        .makerFee(0)
                        .build();

                result.add(symbol);

                //log.debug("{}", symbol);
                i++;
            }
        }
        return result;
    }

    public void close() {
        exchangeCore.shutdown();
    }

    public enum AllowedSymbolTypes {
        FUTURES_CONTRACT,
        CURRENCY_EXCHANGE_PAIR,
        BOTH
    }
}
