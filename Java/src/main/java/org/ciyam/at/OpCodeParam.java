package org.ciyam.at;

import java.nio.ByteBuffer;
import java.util.function.Function;

enum OpCodeParam {

	VALUE(OpCodeParam::compileLong) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Long.valueOf(Utils.getCodeValue(codeByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("#%016x", (Long) value);
		}
	},
	DEST_ADDR(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("@%08x", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	INDIRECT_DEST_ADDR(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("@($%08x)", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	INDIRECT_DEST_ADDR_WITH_INDEX(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("@($%08x", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	SRC_ADDR(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("$%08x", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	INDIRECT_SRC_ADDR(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("$($%08x)", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	INDIRECT_SRC_ADDR_WITH_INDEX(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("$($%08x", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	INDEX(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("+ $%08x)", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	CODE_ADDR(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getCodeAddress(codeByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("[%04x]", (Integer) value);
		}
	},
	OFFSET(OpCodeParam::compileByte) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Byte.valueOf(Utils.getCodeOffset(codeByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("PC+%02x=[%04x]", (int) ((Byte) value), postOpcodeProgramCounter - 1 + (Byte) value);
		}
	},
	FUNC(OpCodeParam::compileFunc) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Short.valueOf(codeByteBuffer.getShort());
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			FunctionCode functionCode = FunctionCode.valueOf((Short) value);

			// generic/unknown form
			if (functionCode == null)
				return String.format("FN(%04x)", (Short) value);

			// API pass-through
			if (functionCode == FunctionCode.API_PASSTHROUGH)
				return String.format("API-FN(%04x)", (Short) value);

			return "\"" + functionCode.name() + "\"" + String.format("{%04x}", (Short) value);
		}
	},
	BLOCK_HEIGHT(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(codeByteBuffer.getInt());
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("height $%08x", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	};

	private final Function<? super Object, byte[]> compiler;

	private OpCodeParam(Function<? super Object, byte[]> compiler) {
		this.compiler = compiler;
	}

	public abstract Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException;

	private static byte[] compileByte(Object o) {
		// Highly likely to be an Integer, so try that first
		try {
			int intValue = (int) o;
			if (intValue < Byte.MIN_VALUE || intValue > Byte.MAX_VALUE)
				throw new ClassCastException("Value too large to compile to byte");

			return new byte[] { (byte) intValue };
		} catch (ClassCastException e) {
			// Try again using Byte
			return new byte[] { (byte) o };
		}
	}

	private static byte[] compileShort(Object o) {
		short s = (short) o;
		return new byte[] { (byte) (s >>> 8), (byte) (s) };
	}

	private static byte[] compileInt(Object o) {
		return MachineState.toByteArray((int) o);
	}

	private static byte[] compileLong(Object o) {
		// Highly likely to be a Long, so try that first
		try {
			return MachineState.toByteArray((long) o);
		} catch (ClassCastException e) {
			// Try again using Integer
			return MachineState.toByteArray((long)(int) o);
		}
	}

	private static byte[] compileFunc(Object o) {
		try {
			FunctionCode func = (FunctionCode) o;
			return compileShort(func.value);
		} catch (ClassCastException e) {
			// Couldn't cast to FunctionCode,
			// but try Short in case caller is using API-PASSTHROUGH range
			return compileShort(o);
		}
	}

	protected byte[] compile(Object arg) {
		return this.compiler.apply(arg);
	}

	public String disassemble(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer, int postOpcodeProgramCounter) throws ExecutionException {
		Object value = fetch(codeByteBuffer, dataByteBuffer);

		return this.toString(value, postOpcodeProgramCounter);
	}

	protected abstract String toString(Object value, int postOpcodeProgramCounter);

}
