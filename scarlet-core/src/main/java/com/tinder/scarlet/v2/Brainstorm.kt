/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import com.tinder.scarlet.Message
import java.lang.reflect.Type


interface Scarlet : ServiceFactory.Factory {

    data class Configuration(
        val protocol: Protocol,
        val lifecycle: Lifecycle,
        val backoffStrategy: BackoffStrategy
    )

    interface Factory {
        fun create(configuration: Configuration): Scarlet
    }
}

interface ServiceFactory {

    fun <T> create(): T

    data class Configuration(
        val topic: Topic,
        val lifecycle: Lifecycle,
        val backoffStrategy: BackoffStrategy,
        val streamAdapters: List<Any> = emptyList(),
        val messageAdapters: List<Any> = emptyList()
    )

    interface Factory {
        fun create(configuration: ServiceFactory.Configuration): ServiceFactory
    }
}

// plugin
interface Protocol {
    fun createChannelFactory(): Channel.Factory

    fun createMessageQueueFactory(): MessageQueue.Factory

    fun createChannelOpenRequestFactory(channel: Channel): OpenRequest.Factory

    fun createChannelCloseRequestFactory(channel: Channel): CloseRequest.Factory

    fun createMessageMetaDataFactory(channel: Channel): MessageMetaData.Factory

    fun createEventAdapterFactory(channel: Channel): EventAdapter.Factory

    interface OpenRequest {
        interface Factory {
            fun create(channel: Channel): OpenRequest
        }
    }

    interface OpenResponse

    interface CloseRequest {
        interface Factory {
            fun create(channel: Channel): CloseRequest
        }
    }

    interface CloseResponse

    interface MessageMetaData {
        interface Factory {
            fun create(channel: Channel): MessageMetaData? = null
        }
    }

    sealed class Event {
        data class OnOpening(
            val channel: Channel, val request: OpenRequest
        ) : Event()

        data class OnOpened(
            val channel: Channel, val request: OpenRequest, val response: OpenResponse
        ) : Event()

        data class OnMessageReceived(
            val channel: Channel, val message: Message, val messageMetaData: MessageMetaData
        ) : Event()

        data class OnClosing(
            val channel: Channel, val request: CloseRequest
        ) : Event()

        data class OnClosed(
            val channel: Channel, val request: CloseRequest, val response: CloseResponse
        ) : Event()

        data class OnCanceled(val channel: Channel, val throwable: Throwable?) : Event()
    }

    interface EventAdapter<T> {
        fun fromEvent(event: Protocol.Event): T

        interface Factory {
            fun create(type: Type, annotations: Array<Annotation>): EventAdapter<*>
        }
    }
}

interface Channel {
    val topic: Topic

    fun open(openRequest: Protocol.OpenRequest)

    fun close(closeRequest: Protocol.CloseRequest)

    fun forceClose()

    interface Listener {
        fun onOpened(channel: Channel, response: Protocol.OpenResponse)
        fun onClosing(channel: Channel)
        fun onClosed(channel: Channel, response: Protocol.CloseResponse)
        fun onCanceled(channel: Channel, throwable: Throwable?)
    }

    interface Factory {
        fun create(topic: Topic, listener: Listener): Channel? = null
    }
}

interface MessageQueue {

    fun send(message: Message, messageMetaData: Protocol.MessageMetaData)

    interface Listener {
        fun onMessageReceived(channel: Channel, message: Message, metadata: Protocol.MessageMetaData? = null)
        fun onMessageDelivered(channel: Channel, message: Message, metadata: Protocol.MessageMetaData? = null)
    }

    interface Factory {
        fun create(channel: Channel, listener: Listener): MessageQueue? = null
    }
}

interface Topic {
    val id: String
}

object DefaultTopic : Topic {
    override val id = ""
}

// plugin
interface Lifecycle {

    // Event

    // onStart()

    // onStop()

}

// plugin
interface BackoffStrategy {
}
