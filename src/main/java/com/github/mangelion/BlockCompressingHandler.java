package com.github.mangelion;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.ReferenceCountUtil;

import static com.github.mangelion.ClickHousePacketDecoder.CH_SERVER_COMPRESSION_LEVEL_ATTRIBUTE;
import static com.github.mangelion.ClickHousePacketDecoder.CH_SERVER_COMPRESSION_METHOD_ATTRIBUTE;

/**
 * @author Camelion
 * @since 21/02/2018
 */
@ChannelHandler.Sharable
final class BlockCompressingHandler extends MessageToByteEncoder<DataBlock> {
    static final BlockCompressingHandler BLOCK_COMPRESSING_HANDLER = new BlockCompressingHandler();

    @Override
    protected void encode(ChannelHandlerContext ctx, DataBlock msg, ByteBuf out) throws Exception {
        CompressionMethod compressionMethod = ctx.channel().attr(CH_SERVER_COMPRESSION_METHOD_ATTRIBUTE).get();
        long level = ctx.channel().attr(CH_SERVER_COMPRESSION_LEVEL_ATTRIBUTE).get();

        DataBlockEncoder.writeHeader(msg, out);

        int inStart;
        ByteBuf in = null;
        try {
            in = ctx.alloc().buffer();
            inStart = in.writerIndex();
            DataBlockEncoder.writeBlock(msg, in);
            // todo: at this point we can switch compression method on-the-fly
            // depending on channel writeability (zstd or lzhc if not writable and vice versa lz4)
            compressionMethod.compress(in, inStart, out, level);
        } finally {
            ReferenceCountUtil.release(in);
        }
    }
}
