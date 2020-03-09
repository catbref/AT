package common;

import java.nio.ByteBuffer;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ciyam.at.MachineState;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class ExecutableTest {

	private static final int DATA_OFFSET = MachineState.HEADER_LENGTH; // code bytes are not present
	private static final int CALL_STACK_OFFSET = DATA_OFFSET + TestUtils.NUM_DATA_PAGES * MachineState.VALUE_SIZE;

	public TestLogger logger;
	public TestAPI api;
	public MachineState state;
	public ByteBuffer codeByteBuffer;
	public ByteBuffer dataByteBuffer;
	public ByteBuffer stateByteBuffer;
	public int callStackSize;
	public int userStackOffset;
	public int userStackSize;

	@BeforeClass
	public static void beforeClass() {
		Security.insertProviderAt(new BouncyCastleProvider(), 0);
	}

	@Before
	public void beforeTest() {
		logger = new TestLogger();
		api = new TestAPI();
		codeByteBuffer = ByteBuffer.allocate(TestUtils.NUM_CODE_PAGES * MachineState.OPCODE_SIZE);
		dataByteBuffer = ByteBuffer.allocate(TestUtils.NUM_DATA_PAGES * MachineState.VALUE_SIZE);
		stateByteBuffer = null;
	}

	@After
	public void afterTest() {
		stateByteBuffer = null;
		codeByteBuffer = null;
		dataByteBuffer = null;
		api = null;
		logger = null;
	}

	protected void execute(boolean onceOnly) {
		byte[] headerBytes = TestUtils.HEADER_BYTES;
		byte[] codeBytes = codeByteBuffer.array();
		byte[] dataBytes = dataByteBuffer.array();

		state = new MachineState(api, logger, headerBytes, codeBytes, dataBytes);

		do {
			System.out.println("Starting execution:");
			System.out.println("Current block height: " + api.getCurrentBlockHeight());

			// Actual execution
			state.execute();

			System.out.println("After execution:");
			System.out.println("Steps: " + state.getSteps());
			System.out.println("Program Counter: " + String.format("%04x", state.getProgramCounter()));
			System.out.println("Stop Address: " + String.format("%04x", state.getOnStopAddress()));
			System.out.println("Error Address: " + (state.getOnErrorAddress() == null ? "not set" : String.format("%04x", state.getOnErrorAddress())));

			if (state.getIsSleeping())
				System.out.println("Sleeping until current block height (" + state.getCurrentBlockHeight() + ") reaches " + state.getSleepUntilHeight());
			else
				System.out.println("Sleeping: " + state.getIsSleeping());

			System.out.println("Stopped: " + state.getIsStopped());
			System.out.println("Finished: " + state.getIsFinished());

			if (state.getHadFatalError())
				System.out.println("Finished due to fatal error!");

			System.out.println("Frozen: " + state.getIsFrozen());

			long newBalance = state.getCurrentBalance();
			System.out.println("New balance: " + newBalance);
			api.setCurrentBalance(newBalance);

			// Bump block height
			api.bumpCurrentBlockHeight();
		} while (!onceOnly && !state.getIsFinished());

		// Ready for diagnosis
		byte[] stateBytes = state.toBytes();

		// We know how the state will be serialized so we can extract values
		// header + data(size * 8) + callStack length(4) + callStack + userStack length(4) + userStack

		stateByteBuffer = ByteBuffer.wrap(stateBytes);
		callStackSize = stateByteBuffer.getInt(CALL_STACK_OFFSET);
		userStackOffset = CALL_STACK_OFFSET + 4 + callStackSize;
		userStackSize = stateByteBuffer.getInt(userStackOffset);
	}

	protected long getData(int address) {
		int index = DATA_OFFSET + address * MachineState.VALUE_SIZE;
		return stateByteBuffer.getLong(index);
	}

	protected int getCallStackPosition() {
		return TestUtils.NUM_CALL_STACK_PAGES * MachineState.ADDRESS_SIZE - callStackSize;
	}

	protected int getCallStackEntry(int address) {
		int index = CALL_STACK_OFFSET + 4 + address - TestUtils.NUM_CALL_STACK_PAGES * MachineState.ADDRESS_SIZE + callStackSize;
		return stateByteBuffer.getInt(index);
	}

	protected int getUserStackPosition() {
		return TestUtils.NUM_USER_STACK_PAGES * MachineState.VALUE_SIZE - userStackSize;
	}

	protected long getUserStackEntry(int address) {
		int index = userStackOffset + 4 + address - TestUtils.NUM_USER_STACK_PAGES * MachineState.VALUE_SIZE + userStackSize;
		return stateByteBuffer.getLong(index);
	}

}
