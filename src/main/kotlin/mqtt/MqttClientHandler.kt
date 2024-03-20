package org.example.mqtt

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.mqtt.*
import org.example.LoadConfig
import org.example.MessageInfoMetrics

class MqttClientHandler(
    private val groupId : Int,
    private val channelId : Int,
    private val topic: String,
    private val loadConfig: LoadConfig,
    private val messageCounter : MessageInfoMetrics?
) : ChannelInboundHandlerAdapter() {

    private var connectMessage: MqttMessage? = null
    private var messageSentCount = 0
    /*private var pubAckCount = 0*/

    init {
        connectMessage = MqttMessageFactory.newMessage(
            MqttFixedHeader(MqttMessageType.CONNECT, false, loadConfig.qos, false, 0),
            MqttConnectVariableHeader("MQTT", 4, true, true, false, 0, false, false, loadConfig.keepAliveSec),
            MqttConnectPayload("group-${groupId}-channel-${channelId}", "", "", "iCureIoTUser","iCureIoTPassword")
        )
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.writeAndFlush(connectMessage)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is MqttMessage) {
            if (msg.fixedHeader().messageType() == MqttMessageType.CONNACK) {
                for (i in 1..loadConfig.nMessagesPerChannel) {
                    val message  = MqttMessageFactory.newMessage(
                        MqttFixedHeader(MqttMessageType.PUBLISH, false, loadConfig.qos, false, 0),
                        MqttPublishVariableHeader(topic, 0),
                        /*Unpooled.buffer().writeBytes(messagePayload.toByteArray())*/
                        Unpooled.buffer().writeBytes(ByteArray(loadConfig.messagePayloadSize))
                    )
                    /*println("PUB ${i}")*/
                    messageSentCount++
                    messageCounter?.increment()
                    ctx.writeAndFlush(message)
                }
                ctx.close()
            } /*else if (msg.fixedHeader().messageType() == MqttMessageType.PUBACK) {
                pubAckCount++
                if (pubAckCount == messageCount) {
                    println("All messages published successfully")
                    ctx.close()
                }
            }*/
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}