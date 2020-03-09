import static common.TestUtils.hexToBytes;

import java.nio.charset.StandardCharsets;

import org.ciyam.at.ExecutionException;
import org.ciyam.at.FunctionCode;
import org.ciyam.at.MachineState;
import org.ciyam.at.OpCode;
import org.junit.Test;

import common.ExecutableTest;
import common.TestUtils;

public class DisassemblyTests extends ExecutableTest {

	private static final String message = "The quick, brown fox jumped over the lazy dog.";
	private static final byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

	@Test
	public void testRMD160disassembly() throws ExecutionException {
		// Data addr 0 for setting values
		dataByteBuffer.putLong(0L);
		// Data addr 1 for results
		dataByteBuffer.putLong(0L);

		// Data addr 2 has start of message bytes (address 4)
		dataByteBuffer.putLong(4L);

		// Data addr 3 has length of message bytes
		dataByteBuffer.putLong(messageBytes.length);

		// Data addr 4+ for message
		dataByteBuffer.put(messageBytes);

		// RMD160 of "The quick, brown fox jumped over the lazy dog." is b5a4b1898af3745dbbb5becb83e72787df9952c9
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("00000000b5a4b189"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B1.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("8af3745dbbb5becb"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B2.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("83e72787df9952c9"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B3.value).putInt(0);

		codeByteBuffer.put(OpCode.EXT_FUN_RET_DAT_2.value).putShort(FunctionCode.CHECK_RMD160_WITH_B.value).putInt(1).putInt(2).putInt(3);

		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.ECHO.value).putInt(1);

		codeByteBuffer.put(OpCode.FIN_IMD.value);

		byte[] headerBytes = TestUtils.HEADER_BYTES;
		byte[] codeBytes = codeByteBuffer.array();
		byte[] dataBytes = dataByteBuffer.array();

		state = new MachineState(api, logger, headerBytes, codeBytes, dataBytes);

		System.out.println(state.disassemble());
	}

}
