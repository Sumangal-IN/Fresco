package com.example.fresco;

import java.util.List;

public class Question {
	Question(int questionID) {
		this.questionID = questionID;
		this.selectedAnswer = 0;
	}

	int questionID;
	List<Answer> answers;
	int selectedAnswer;

	@Override
	public String toString() {
		return "Question [questionID=" + questionID + ", answers=" + answers + ", selectedAnswer=" + selectedAnswer
				+ "]";
	}

};