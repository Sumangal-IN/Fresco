package com.example.fresco;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

@SpringBootApplication
@RestController
public class FrescoApplication {
	static RestTemplate restTemplate = new RestTemplate();
	public static void main(String[] args) {
		SpringApplication.run(FrescoApplication.class, args);
	}
	
	@RequestMapping(value = "/complete/{xpi}/{courseid}")
	public String completeCourse(@PathVariable("xpi") String xpi, @PathVariable("courseid") String courseid) {
		String XAPIKey = xpi;
		String courses[] = { courseid };// {"141","174"};//{"431","378","345","443","240","269","112"};//{"91","140"};//{"438","407"};//{"449","442","406","400","122","370"};//{"408","234","250"};//{
										// "202","231","257"
										// };//{"373","381","420","384","454"};//{"423"};//{"217","76","170","201","248"};//{"329","330","375","364"};//{"235","167","168","433","311","372","166"};//{
										// "83", "244", "247" };
		// String courseID = "83";
		for (int i = 0; i < courses.length; i++) {
			for (JsonElement task : getTasks(courses[i], XAPIKey)) {
				String taskID = task.getAsJsonObject().get("id").toString();
				completeTasks(taskID, courses[i], XAPIKey);
			}
		}
		return "Done";
	}

//	public static void main(String[] args) {
//
//		String XAPIKey = "g9190xCegQzTcGTer4o56adZr13B2V6_KDTkW7adSgs";
//		String courses[] = { "13", "11" };// {"141","174"};//{"431","378","345","443","240","269","112"};//{"91","140"};//{"438","407"};//{"449","442","406","400","122","370"};//{"408","234","250"};//{
//											// "202","231","257"
//											// };//{"373","381","420","384","454"};//{"423"};//{"217","76","170","201","248"};//{"329","330","375","364"};//{"235","167","168","433","311","372","166"};//{
//											// "83", "244", "247" };
//		// String courseID = "83";
//		for (int i = 0; i < courses.length; i++) {
//			for (JsonElement task : getTasks(courses[i], XAPIKey)) {
//				String taskID = task.getAsJsonObject().get("id").toString();
//				completeTasks(taskID, courses[i], XAPIKey);
//			}
//		}
//	}

	private static void completeTasks(String taskID, String courseID, String XAPIKey) {
		for (JsonElement content : getContents(taskID, XAPIKey)) {
			String contentID = content.getAsJsonObject().get("id").toString();
			String contentType = content.getAsJsonObject().get("content_type").toString();
			System.out.println(contentID + ":" + contentType);
			if (contentType.equals("\"quiz\"")) {
				completeQuiz(courseID, contentID, XAPIKey);
			}
		}
		markDone(taskID, courseID, XAPIKey);
	}

	private static void markDone(String taskID, String courseID, String XAPIKey) {
		String progressID = getProgress(courseID, XAPIKey);
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Api-Key", XAPIKey);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		String url = "https://play-api.fresco.me/api/v1/progresses/" + progressID + "/mark_status.json?data_id="
				+ taskID + "&data_type=Task&id=" + progressID;
		System.out.println(url);

		ConnectionKeepAliveStrategy myStrategy = new ConnectionKeepAliveStrategy() {
			@Override
			public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
				HeaderElementIterator it = new BasicHeaderElementIterator(
						response.headerIterator(HTTP.CONN_KEEP_ALIVE));
				while (it.hasNext()) {
					HeaderElement he = it.nextElement();
					String param = he.getName();
					String value = he.getValue();
					if (value != null && param.equalsIgnoreCase("timeout")) {
						return Long.parseLong(value) * 1000;
					}
				}
				return 5 * 1000;
			}
		};

		RestTemplate rt = new RestTemplate();

		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
		CloseableHttpClient client = HttpClients.custom().setKeepAliveStrategy(myStrategy)
				.setConnectionManager(connManager).build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(client);
		requestFactory.setConnectTimeout(5000);
		requestFactory.setReadTimeout(5000);
		rt.setRequestFactory(requestFactory);
		try {
			ResponseEntity<String> res = rt.exchange(url, HttpMethod.PATCH, entity, String.class);
			System.out.println(res.getBody());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private static JsonArray getContents(String taskID, String XAPIKey) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Api-Key", XAPIKey);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		String url = "https://play-api.fresco.me/api/v1/tasks/" + taskID + ".json";
		ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		System.out.println(res.getBody());
		JsonParser parser = new JsonParser();
		return parser.parse(res.getBody()).getAsJsonObject().get("contents").getAsJsonArray();
	}

	private static JsonArray getTasks(String courseID, String XAPIKey) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Api-Key", XAPIKey);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		String url = "https://play-api.fresco.me/api/v1/nodes/" + courseID + ".json";
		ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		JsonParser parser = new JsonParser();
		return parser.parse(res.getBody()).getAsJsonObject().get("tasks").getAsJsonArray();
	}

	private static void completeQuiz(String courseID, String contentID, String XAPIKey) {
		String progressID = getProgress(courseID, XAPIKey);
		int passMarks = Integer.parseInt(getPassMarks(contentID, XAPIKey));
		String sectionID = getSectionID(contentID, XAPIKey);
		System.out.println(sectionID);

		List<Question> questionPaper = new ArrayList<>();
		for (JsonElement question : getQuestionPaper(contentID, XAPIKey)) {
			Question ques = new Question(Integer.parseInt(question.getAsJsonObject().get("id").toString()));
			List<Answer> answers = new ArrayList<>();
			for (JsonElement answer : question.getAsJsonObject().get("answers").getAsJsonArray()) {
				Answer ans = new Answer(Integer.parseInt(answer.getAsJsonObject().get("id").toString()));
				answers.add(ans);
			}
			ques.answers = answers;
			questionPaper.add(ques);
		}

		System.out.println(questionPaper);

		int questionNo = 0;
		int currentMarks = evaluate(progressID, contentID, sectionID, XAPIKey, questionPaper);
		while (currentMarks < /* passMarks */questionPaper.size()) {
			System.out.println(currentMarks);
			System.out.println(questionPaper.size());
			boolean firstAnswerCorrect = true;
			for (int ansNo = 1; ansNo < questionPaper.get(questionNo).answers.size(); ansNo++) {
				questionPaper.get(questionNo).selectedAnswer = ansNo;
				int newMarks = evaluate(progressID, contentID, sectionID, XAPIKey, questionPaper);
				if (newMarks > currentMarks) {
					currentMarks = newMarks;
					firstAnswerCorrect = false;
					break;
				}
			}
			if (firstAnswerCorrect)
				questionPaper.get(questionNo).selectedAnswer = 0;
			questionNo++;
		}
		System.out.println(questionPaper);
	}

	private static int evaluate(String progressID, String contentID, String sectionID, String XAPIKey,
			List<Question> questionPaper) {
		JsonParser parser = new JsonParser();
		JsonElement body = parser.parse("{}");
		body.getAsJsonObject().add("progress_id", parser.parse(progressID));
		body.getAsJsonObject().add("content_id", parser.parse(contentID));
		body.getAsJsonObject().add("sections", parser.parse("[]"));
		JsonElement section = parser.parse("{}");
		section.getAsJsonObject().add("section_id", parser.parse(sectionID));
		section.getAsJsonObject().add("questions", parser.parse("[]"));
		for (int i = 0; i < questionPaper.size(); i++) {
			JsonElement question = parser.parse("{}");
			question.getAsJsonObject().add("question_id",
					parser.parse(Integer.toString(questionPaper.get(i).questionID)));
			question.getAsJsonObject()
					.add("answer_ids",
							parser.parse("[" + Integer.toString(
									questionPaper.get(i).answers.get(questionPaper.get(i).selectedAnswer).answerID)
									+ "]"

							));
			section.getAsJsonObject().get("questions").getAsJsonArray().add(question);
		}

		body.getAsJsonObject().get("sections").getAsJsonArray().add(section);
		String requestBody = "{\"data\":\"" + body.toString().replaceAll("\"", "\\\\\\\"") + "\"}";
		System.out.println(requestBody);

		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Api-Key", XAPIKey);
		headers.set("Content-Type", "application/json;charset=UTF-8");
		HttpEntity<String> entity = new HttpEntity<String>(requestBody, headers);
		String url = "https://play-api.fresco.me/api/v1/assessments/post_result.json";
		ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
		System.out.println(res.getBody());
		return Integer.parseInt(parser.parse(res.getBody()).getAsJsonObject().get("assessment").getAsJsonObject()
				.get("user_score").toString());
	}

	static String getSectionID(String contentID, String XAPIKey) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Api-Key", XAPIKey);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		String url = "https://play-api.fresco.me/api/v1/assessments/get_quiz_details.json?id=" + contentID;
		ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		JsonParser parser = new JsonParser();
		return parser.parse(res.getBody()).getAsJsonObject().get("assessment").getAsJsonObject().get("sections")
				.getAsJsonArray().get(0).getAsJsonObject().get("id").toString();
	}

	static String getPassMarks(String contentID, String XAPIKey) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Api-Key", XAPIKey);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		String url = "https://play-api.fresco.me/api/v1/assessments/get_quiz_details.json?id=" + contentID;
		ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		JsonParser parser = new JsonParser();
		System.out.println(res.getBody());
		return parser.parse(res.getBody()).getAsJsonObject().get("assessment").getAsJsonObject().get("passing_marks")
				.toString();
	}

	static JsonArray getQuestionPaper(String contentID, String XAPIKey) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Api-Key", XAPIKey);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		String url = "https://play-api.fresco.me/api/v1/assessments/get_quiz_details.json?id=" + contentID;
		ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		System.out.println(res.getBody());
		JsonParser parser = new JsonParser();
		String body = res.getBody();
		return parser.parse(body).getAsJsonObject().get("assessment").getAsJsonObject().get("sections").getAsJsonArray()
				.get(0).getAsJsonObject().get("questions").getAsJsonArray();
	}

	static String getProgress(String courseID, String XAPIKey) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Api-Key", XAPIKey);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		String url = "https://play-api.fresco.me/api/v1/nodes/" + courseID + ".json";
		ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		JsonParser parser = new JsonParser();
		return parser.parse(res.getBody()).getAsJsonObject().get("progress").getAsJsonObject().get("id").toString();
	}
}
