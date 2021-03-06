package ua.kpi.nc.controller.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ua.kpi.nc.persistence.dto.ReportQuestionDto;
import ua.kpi.nc.persistence.model.FormQuestion;
import ua.kpi.nc.persistence.model.Role;
import ua.kpi.nc.persistence.model.enums.RoleEnum;
import ua.kpi.nc.report.Report;
import ua.kpi.nc.report.renderer.RendererFactory;
import ua.kpi.nc.report.renderer.ReportRenderer;
import ua.kpi.nc.service.FormQuestionService;
import ua.kpi.nc.service.ReportService;
import ua.kpi.nc.service.RoleService;
import ua.kpi.nc.service.ServiceFactory;

/**
 * Created by dima on 23.04.16.
 */
@RestController
@RequestMapping("/admin")
public class AdminReportsController {

	private static final String CONTENT_DISPOSITION_NAME = "Content-Disposition";
	private static final String JSON_MIME_TYPE = "application/json";
	private static final String XLS_MIME_TYPE = "application/vnd.ms-excel";
	private static final String XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private static Logger log = LoggerFactory.getLogger(AdminReportsController.class.getName());

	private ReportService service = ServiceFactory.getReportService();

	@RequestMapping(value = "reports/approved.{format}", method = RequestMethod.GET)
	public void generateReportOfApproved(@PathVariable String format, HttpServletResponse response) {
		log.info("Generating report of amount of approved with format = {}", format);
		Report report = service.getReportOfApproved();
		renderReport(report, format, response);
	}

	@RequestMapping(value = "reports/answers.{format}/{questionId}", method = RequestMethod.GET)
	public void generateReportOfAnswers(@PathVariable String format, @PathVariable Long questionId,
			HttpServletResponse response) {
		log.info("Generating report of answers with format = {} for question with id = {}", format, questionId);
		if (questionId != null) {
			Report report = service.getReportOfAnswers(questionId);
			renderReport(report, format, response);
		}
	}

	private void renderReport(Report report, String format, HttpServletResponse response) {
		ReportRenderer renderer = null;
		String mimeType = null;
		switch (format) {
		case "json":
			renderer = RendererFactory.getJSONRenderer();
			mimeType = JSON_MIME_TYPE;
			break;
		case "xls":
			renderer = RendererFactory.getXLSRenderer();
			mimeType = XLS_MIME_TYPE;
			response.setHeader(CONTENT_DISPOSITION_NAME, "inline; filename=\"" + report.getTitle() + ".xls\"");
			break;
		case "xlsx":
			renderer = RendererFactory.getXLSXRenderer();
			mimeType = XLSX_MIME_TYPE;
			response.setHeader(CONTENT_DISPOSITION_NAME, "inline; filename=\"" + report.getTitle() + ".xlsx\"");
			break;
		}
		if (mimeType != null) {
			response.setContentType(mimeType);
		}
		if (renderer != null) {
			try {
				renderer.render(report, response.getOutputStream());
			} catch (IOException e) {
				log.error("Error during rendering report.", e);
			}
		}
	}

	@RequestMapping(value = "reports/answers/questions")
	public List<ReportQuestionDto> getQuestions() {
		FormQuestionService questionService = ServiceFactory.getFormQuestionService();
		RoleService roleService = ServiceFactory.getRoleService();
		Role studentRole = roleService.getRoleByTitle(RoleEnum.valueOf(RoleEnum.ROLE_STUDENT));
		List<FormQuestion> questions = questionService.getWithVariantsByRole(studentRole);
		return getReportQuestionsDto(questions);
	}

	private List<ReportQuestionDto> getReportQuestionsDto(List<FormQuestion> formQuestions) {
		List<ReportQuestionDto> questionDtos = new ArrayList<>();
		for (FormQuestion formQuestion : formQuestions) {
			questionDtos.add(new ReportQuestionDto(formQuestion.getId(), formQuestion.getTitle()));

		}
		return questionDtos;
	}

}
