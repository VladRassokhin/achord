package com.github.mangelion;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.ReferenceCountUtil;

import java.util.List;

/**
 * @author Camelion
 * @since 19/02/2018
 */
@ChannelHandler.Sharable
final class BlockDecompressingHandler extends MessageToMessageDecoder<CompressedBlock> {
    static final BlockDecompressingHandler BLOCK_DECOMPRESSING_HANDLER = new BlockDecompressingHandler();

    @Override
    protected void decode(ChannelHandlerContext ctx, CompressedBlock compressedBlock, List<Object> out) throws Exception {
        ByteBuf decompressed;

        switch (compressedBlock.method) {
            case 0x82:
                decompressed = CompressionMethod.LZ4.decompress(compressedBlock.compressed, compressedBlock.decompressedSize, ctx.alloc());
                break;
            case 0x90:
                decompressed = CompressionMethod.ZSTD.decompress(compressedBlock.compressed, compressedBlock.decompressedSize, ctx.alloc());
                break;
            case 0x02:
                decompressed = CompressionMethod.NONE.decompress(compressedBlock.compressed, compressedBlock.decompressedSize, ctx.alloc());
                break;
            default:
                throw new IllegalStateException("Unknown compression method [" + Integer.toHexString(compressedBlock.method) + "]");
        }

        try {
            DataBlock block = ClickHousePacketDecoder.readBlock(ctx, decompressed);
            out.add(block);
        } finally {
            ReferenceCountUtil.release(decompressed);
        }
    }
}
