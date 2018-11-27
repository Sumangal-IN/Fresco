package com.example.fresco;

class Answer {
	Answer(int answerID) {
		this.answerID = answerID;
		correct = false;
	}

	int answerID;
	boolean correct;

	@Override
	public String toString() {
		return "Answer [answerID=" + answerID + ", correct=" + correct + "]";
	}

};