package org.ciyam.at;

import java.nio.ByteBuffer;

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

	@FunctionalInterface
	private interface Compiler {
		byte[] compile(OpCode opCode, Object arg);
	}
	private final Compiler compiler;

	private OpCodeParam(Compiler compiler) {
		this.compiler = compiler;
	}

	public abstract Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException;

	private static byte[] compileByte(OpCode opcode, Object arg) {
		// Highly likely to be an Integer, so try that first
		try {
			int intValue = (int) arg;
			if (intValue < Byte.MIN_VALUE || intValue > Byte.MAX_VALUE)
				throw new ClassCastException("Value too large to compile to byte");

			return new byte[] { (byte) intValue };
		} catch (ClassCastException e) {
			// Try again using Byte
			return new byte[] { (byte) arg };
		}
	}

	private static byte[] compileShort(OpCode opcode, Object arg) {
		short s = (short) arg;
		return new byte[] { (byte) (s >>> 8), (byte) (s) };
	}

	private static byte[] compileInt(OpCode opcode, Object arg) {
		return MachineState.toByteArray((int) arg);
	}

	private static byte[] compileLong(OpCode opcode, Object arg) {
		// Highly likely to be a Long, so try that first
		try {
			return MachineState.toByteArray((long) arg);
		} catch (ClassCastException e) {
			// Try again using Integer
			return MachineState.toByteArray((long)(int) arg);
		}
	}

	private static byte[] compileFunc(OpCode opcode, Object arg) {
		try {
			FunctionCode func = (FunctionCode) arg;
			opcode.preExecuteCheck(func.value);
			return compileShort(opcode, func.value);
		} catch (ClassCastException e) {
			// Couldn't cast to FunctionCode,
			// but try Short in case caller is using API-PASSTHROUGH range
			return compileShort(opcode, arg);
		} catch (ExecutionException e) {
			// Wrong opcode for this function
			throw new ClassCastException("Wrong opcode for this function");
		}
	}

	protected byte[] compile(OpCode opcode, Object arg) {
		return this.compiler.compile(opcode, arg);
	}

	public String disassemble(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer, int postOpcodeProgramCounter) throws ExecutionException {
		Object value = fetch(codeByteBuffer, dataByteBuffer);

		return this.toString(value, postOpcodeProgramCounter);
	}

	protected abstract String toString(Object value, int postOpcodeProgramCounter);

}
