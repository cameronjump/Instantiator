package com.hannesdorfmann.instantiator

import java.time.*
import java.util.Date
import kotlin.experimental.and
import kotlin.random.Random
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.withNullability

private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

private object IntInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<Int> {
    override val type: KType = Int::class.createType()
    override fun createInstance(random: Random): Int = random.nextInt()
}

private object BooleanInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<Boolean> {
    override val type: KType = Boolean::class.createType()
    override fun createInstance(random: Random): Boolean = random.nextBoolean()
}

private object FloatInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<Float> {
    override val type: KType = Float::class.createType()
    override fun createInstance(random: Random): Float = random.nextFloat()
}

private object DoubleInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<Double> {
    override val type: KType = Double::class.createType()
    override fun createInstance(random: Random): Double = random.nextDouble()
}

private object LongInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<Long> {
    override val type: KType = Long::class.createType()
    override fun createInstance(random: Random): Long = random.nextLong()
}

private object ShortInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<Short> {
    override val type: KType = Short::class.createType()
    override fun createInstance(random: Random): Short = random.nextInt(Short.MAX_VALUE.toInt()).toShort()
}

private object ByteInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<Byte> {
    override val type: KType = Byte::class.createType()
    override fun createInstance(random: Random): Byte = random.nextBytes(1)[0]
}

private object StringInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<String> {
    override val type: KType = String::class.createType()
    override fun createInstance(random: Random): String {
        val bytes = random.nextBytes(10)
        return (bytes.indices)
            .map { i ->
                charPool[(bytes[i] and 0xFF.toByte() and (charPool.size - 1).toByte()).toInt()]
            }.joinToString("")
    }
}

private object CharInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<Char> {
    override val type: KType = Char::class.createType()
    override fun createInstance(random: Random): Char = charPool[random.nextInt(charPool.size)]
}

private object DateInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<Date> {
    override val type: KType = Date::class.createType()

    override fun createInstance(random: Random): Date = Date(random.nextLong())
}

private object InstantInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<Instant> {

    override val type: KType = Instant::class.createType()

    override fun createInstance(random: Random): Instant =
        Instant.ofEpochMilli(random.nextLong(4102441200000)) // 2100-01-01
}


private object LocalDateTimeInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<LocalDateTime> {

    override val type: KType = LocalDateTime::class.createType()

    override fun createInstance(random: Random): LocalDateTime =
        LocalDateTime.ofInstant(
            InstantInstanceFactory.createInstance(random),
            ZoneId.of(ZoneId.getAvailableZoneIds().random(random))
        )
}

private object LocalDateInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<LocalDate> {

    override val type: KType = LocalDate::class.createType()

    override fun createInstance(random: Random): LocalDate =
        LocalDateTimeInstanceFactory.createInstance(random).toLocalDate()
}

private object LocalTimeInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<LocalTime> {

    override val type: KType = LocalTime::class.createType()

    override fun createInstance(random: Random): LocalTime =
        LocalDateTimeInstanceFactory.createInstance(random).toLocalTime()
}

private object ZonedDateTimeInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<ZonedDateTime> {

    override val type: KType = ZonedDateTime::class.createType()

    override fun createInstance(random: Random): ZonedDateTime =
        LocalDateTimeInstanceFactory.createInstance(random)
            .atZone(ZoneId.of(ZoneId.getAvailableZoneIds().random(random)))
}

private object OffsetDateTimeInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<OffsetDateTime> {

    override val type: KType = OffsetDateTime::class.createType()

    override fun createInstance(random: Random): OffsetDateTime =
        LocalDateTimeInstanceFactory.createInstance(random).atOffset(
            ZoneOffset.ofTotalSeconds(
                random.nextInt(
                    ZoneOffset.MIN.totalSeconds,
                    ZoneOffset.MAX.totalSeconds
                )
            )
        )
}

private object OffsetTimeInstanceFactory : InstantiatorConfig.NonNullableInstanceFactory<OffsetTime> {

    override val type: KType = OffsetTime::class.createType()

    override fun createInstance(random: Random): OffsetTime =
        OffsetDateTimeInstanceFactory.createInstance(random).toOffsetTime()
}

private val IntNullInstanceFactory = IntInstanceFactory.toNullableInstanceFactory()
private val BooleanNullInstanceFactory = BooleanInstanceFactory.toNullableInstanceFactory()
private val FloatNullInstanceFactory = FloatInstanceFactory.toNullableInstanceFactory()
private val DoubleNullInstanceFactory = DoubleInstanceFactory.toNullableInstanceFactory()
private val LongNullInstanceFactory = LongInstanceFactory.toNullableInstanceFactory()
private val ShortNullInstanceFactory = ShortInstanceFactory.toNullableInstanceFactory()
private val ByteNullInstanceFactory = ByteInstanceFactory.toNullableInstanceFactory()
private val StringNullInstanceFactory = StringInstanceFactory.toNullableInstanceFactory()
private val CharNullInstanceFactory = CharInstanceFactory.toNullableInstanceFactory()
private val DateNullInstanceFactory = DateInstanceFactory.toNullableInstanceFactory()
private val InstantNullableInstanceFactory = InstantInstanceFactory.toNullableInstanceFactory()
private val LocalDateTimeNullableInstanceFactory = LocalDateTimeInstanceFactory.toNullableInstanceFactory()
private val LocalDateNullableInstanceFactory = LocalDateInstanceFactory.toNullableInstanceFactory()
private val LocalTimeNullInstanceFactory = LocalTimeInstanceFactory.toNullableInstanceFactory()
private val ZonedDateTimeNullInstanceFactory = ZonedDateTimeInstanceFactory.toNullableInstanceFactory()
private val OffsetDateTimeNullInstanceFactory = OffsetDateTimeInstanceFactory.toNullableInstanceFactory()
private val OffsetTimeNullInstanceFactory = OffsetTimeInstanceFactory.toNullableInstanceFactory()

class InstantiatorConfig(
    val useDefaultArguments: Boolean = true,
    val useNull: Boolean = true,
    val random: Random = Random,
    val numberOfItemsToFull: Int = 10,
    vararg factories: InstanceFactory = DEFAULT_INSTANCE_FACTORIES
) {

    constructor(
        vararg factories: InstanceFactory = DEFAULT_INSTANCE_FACTORIES
    ) : this(useDefaultArguments = true, useNull = true, random = Random, factories = factories)

    internal val instanceFactory: Map<KType, InstanceFactory>

    init {
        if (!factories.contentEquals(DEFAULT_INSTANCE_FACTORIES)) {
            // SLOW TRACK: adding nullable factories automatically

            // Identify which non-nullable factory HAS NO nullable factory counterpart (and vice versa)
            val nullableFactories = mutableMapOf<KType, NullableInstanceFactory<*>>()
            val nonNullableFactories = mutableMapOf<KType, NonNullableInstanceFactory<*>>()
            for (factory in factories) {
                if (factory.type.isMarkedNullable) {
                    if (factory is NullableInstanceFactory<*>) {
                        nullableFactories[factory.type] = factory
                    } else {
                        throw IllegalArgumentException(
                            "A factory's type is marked as nullable but is not of type " +
                                    "${NullableInstanceFactory::class.java}. Type = ${factory.type}. Factory = $factory"
                        )

                    }
                } else {
                    if (factory is NonNullableInstanceFactory<*>) {
                        nonNullableFactories[factory.type] = factory
                    } else {
                        throw IllegalArgumentException(
                            "A factory's type is marked as non-nullable but is not of type " +
                                    "${NonNullableInstanceFactory::class.java}. Type = ${factory.type}. Factory = $factory"
                        )
                    }
                }
            }

            // actually create factories
            val factoriesToAdd = mutableListOf<InstanceFactory>()
            for ((type, factory) in nonNullableFactories) {
                val nullFactory = nullableFactories[type.withNullability(true)]
                if (nullFactory == null) {
                    // no null factory for this type passed in original factories
                    factoriesToAdd.add(factory.toNullableInstanceFactory())
                }
            }

            val allFactories: List<InstanceFactory> = factories.asList() + factoriesToAdd
            instanceFactory = allFactories.associateBy { it.type }

        } else {
            // Fast track: It is factories === DEFAULT_INSTANCE_FACTORIES
            // that means factories contains also a NullableInstanceFactory for each factory
            instanceFactory = factories.associateBy { it.type }
        }

    }

    /**
     * Adds a copy of this [InstantiatorConfig] and then adds the [InstanceFactory] to the new config.
     */
    fun add(vararg factories: InstanceFactory): InstantiatorConfig = InstantiatorConfig(
        useDefaultArguments = this.useDefaultArguments,
        useNull = this.useNull,
        factories = (this.instanceFactory.values + factories).toTypedArray()
    )

    operator fun plus(factory: InstanceFactory): InstantiatorConfig = add(factory)


    companion object {
        val DEFAULT_INSTANCE_FACTORIES: Array<InstanceFactory> = arrayOf(
            IntInstanceFactory,
            IntNullInstanceFactory,
            BooleanInstanceFactory,
            BooleanNullInstanceFactory,
            FloatInstanceFactory,
            FloatNullInstanceFactory,
            DoubleInstanceFactory,
            DoubleNullInstanceFactory,
            StringInstanceFactory,
            StringNullInstanceFactory,
            CharInstanceFactory,
            CharNullInstanceFactory,
            LongInstanceFactory,
            LongNullInstanceFactory,
            ShortInstanceFactory,
            ShortNullInstanceFactory,
            ByteInstanceFactory,
            ByteNullInstanceFactory,
            DateInstanceFactory,
            DateNullInstanceFactory,
            InstantInstanceFactory,
            InstantNullableInstanceFactory,
            LocalDateTimeInstanceFactory,
            LocalDateTimeNullableInstanceFactory,
            LocalDateInstanceFactory,
            LocalDateNullableInstanceFactory,
            LocalTimeInstanceFactory,
            LocalTimeNullInstanceFactory,
            ZonedDateTimeInstanceFactory,
            ZonedDateTimeNullInstanceFactory,
            OffsetDateTimeInstanceFactory,
            OffsetDateTimeNullInstanceFactory,
            OffsetTimeInstanceFactory,
            OffsetTimeNullInstanceFactory
        )
    }

    sealed interface InstanceFactory {
        val type: KType
    }

    interface NonNullableInstanceFactory<T : Any> : InstanceFactory {
        fun createInstance(random: Random): T
    }


    interface NullableInstanceFactory<T> : InstanceFactory {
        fun createInstance(random: Random): T?
    }
}

/**
 * Used by [toNullableInstanceFactory]
 */
enum class ToNullableInstanceFactoryMode {
    /**
     * It randomly decides if value should be null or not
     */
    RANDOM,

    /**
     * It specifies that the returned instance of this
     * [com.hannesdorfmann.instantiator.InstantiatorConfig.NullableInstanceFactory] is always `null`
     */
    ALWAYS_NULL,

    /**
     * It specifies that the returned instance of this
     * [com.hannesdorfmann.instantiator.InstantiatorConfig.NullableInstanceFactory] is never `null`, so always an
     * non-null instance is returned for sure.
     */
    NEVER_NULL
}


/**
 * This is a little utility function that helps to convert a regular
 * [com.hannesdorfmann.instantiator.InstantiatorConfig.NonNullableInstanceFactory] to a
 * [com.hannesdorfmann.instantiator.InstantiatorConfig.NullableInstanceFactory].
 *
 * It does so by delegating the true value creating to the original
 * [com.hannesdorfmann.instantiator.InstantiatorConfig.NonNullableInstanceFactory]
 */
fun <T : Any> InstantiatorConfig.NonNullableInstanceFactory<T>.toNullableInstanceFactory(mode: ToNullableInstanceFactoryMode = ToNullableInstanceFactoryMode.RANDOM): InstantiatorConfig.NullableInstanceFactory<T> {
    val self = this

    return object : InstantiatorConfig.NullableInstanceFactory<T> {
        override val type: KType = self.type.withNullability(true)
        override fun createInstance(random: Random): T? =
            when (mode) {
                ToNullableInstanceFactoryMode.RANDOM -> if (random.nextBoolean()) self.createInstance(random) else null
                ToNullableInstanceFactoryMode.ALWAYS_NULL -> null
                ToNullableInstanceFactoryMode.NEVER_NULL -> self.createInstance(random)
            }
    }
}
