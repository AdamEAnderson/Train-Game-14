package train;

import java.util.EmptyStackException;
import java.util.Stack;

class UndoRedoStack {
	Stack<String> stack;
	String error;			// thrown in case of stack underflow error

	UndoRedoStack(String error) {
		this.stack = new Stack<String>();
		this.error = error;
	}

	void push(String gameState) { 
		stack.push(gameState); 
	}
		
	String pop() throws GameException
	{ 
		String gameString = null;
		try {
			gameString = stack.pop(); 
		} catch (EmptyStackException e) {
			throw new GameException(error);
		}
		return gameString;
	}
	
	void clear() {
		stack.clear();
	}	
}

