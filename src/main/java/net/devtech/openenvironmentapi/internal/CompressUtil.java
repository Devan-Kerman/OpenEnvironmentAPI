package net.devtech.openenvironmentapi.internal;

public class CompressUtil {
	static final byte[] LOG_TABLE_256 = new byte[256];
	private static final long[] FLAGS;

	static {
		FLAGS = new long[64 * 64];
		for (int offset = 0; offset < 64; offset++) {
			for (int bits = 0; bits < 64; bits++) {
				FLAGS[(offset << 6) + bits] = ((1L << bits) - 1) << offset;
			}
		}
	}

	// https://graphics.stanford.edu/~seander/bithacks.html#IntegerLogLookup (translated to java)
	static {
		int index = 0;
		// @formatter:off
		System.arraycopy(new byte[] {-1, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3}, 0, LOG_TABLE_256, 0, 16);
		// @formatter:on
		index += 16;
		index = L2(4, index);

		index = L2(5, index);
		index = L2(5, index);

		index = L2(6, index);
		index = L2(6, index);
		index = L2(6, index);
		index = L2(6, index);

		index = L2(7, index);
		index = L2(7, index);
		index = L2(7, index);
		index = L2(7, index);
		index = L2(7, index);
		index = L2(7, index);
		index = L2(7, index);
		index = L2(7, index);
	}
	private static int L2(int n, int offset) {
		for (int i = 0; i < 16; i++) {
			LOG_TABLE_256[offset + i] = (byte) n;
		}
		return offset + 16;
	}

	/**
	 * @return + 1 = bits
	 */
	public static int logBase2(int v) {
		int t, tt; // temporaries

		if ((tt = v >>> 16) != 0) {
			return (t = tt >> 8) != 0 ? 24 + LOG_TABLE_256[t] : 16 + LOG_TABLE_256[tt];
		} else {
			return (t = v >>> 8) != 0 ? 8 + LOG_TABLE_256[t] : LOG_TABLE_256[v];
		}
	}

	public static long[] resize(long[] data, int len, int oldBits, int newBits) {
		int allocLen = len * newBits;
		if((allocLen & 63) == 0) {
			allocLen >>>= 6;
		} else {
			allocLen = (allocLen >>> 6) + 1;
		}

		long[] alloc = new long[allocLen];
		for (int i = 0; i < len; i++) {
			set(alloc, i, get(data, i, oldBits), newBits);
		}

		return alloc;
	}

	/**
	 * this assumes the array has already been resized
	 */
	public static void set(long[] data, int index, long value, int bits) {
		int bitIndex = index * bits;
		int arrayIndex = bitIndex >> 6;
		int offset = bitIndex & 63;
		if (offset + bits > 64) {
			int firstBits = 64 - offset;

			// encode missing bits
			long next = data[arrayIndex];
			next &= ~FLAGS[(offset << 6) + firstBits];
			next |= value << offset;
			data[arrayIndex] = next;

			long at = data[++arrayIndex];
			at &= ~FLAGS[bits - firstBits];
			at |= value >>> firstBits;
			data[arrayIndex] = at;
		} else {
			long at = data[arrayIndex];
			at &= ~FLAGS[(offset << 6) + bits];
			at |= value << offset;
			data[arrayIndex] = at;
		}
	}

	public static long get(long[] data, int index, int bits) {
		if (data == null) {
			return 0;
		}

		int bitIndex = index * bits;
		int arrayIndex = bitIndex >> 6;
		int offset = bitIndex & 63;
		if (offset + bits > 64) {
			int firstBits = 64 - offset;
			long bottom = (data[arrayIndex] & FLAGS[(offset << 6) + firstBits]) >>> offset;
			long top = (data[arrayIndex + 1] & FLAGS[bits - firstBits]) << firstBits;
			return top | bottom;
		} else {
			return (data[arrayIndex] & FLAGS[(offset << 6) + bits]) >>> offset;
		}
	}
}
