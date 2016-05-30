package ua.kpi.nc.controller.admin;

import com.google.gson.Gson;
import com.itextpdf.text.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ua.kpi.nc.persistence.dao.impl.UserDaoImpl;
import ua.kpi.nc.persistence.model.ApplicationForm;
import ua.kpi.nc.persistence.model.Interview;
import ua.kpi.nc.persistence.model.Role;
import ua.kpi.nc.persistence.model.adapter.GsonFactory;
import ua.kpi.nc.service.ApplicationFormService;
import ua.kpi.nc.service.InterviewService;
import ua.kpi.nc.service.ServiceFactory;
import ua.kpi.nc.util.export.ExportApplicationForm;
import ua.kpi.nc.util.export.ExportApplicationFormImp;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Korzh
 */

@RestController
@RequestMapping("/admin")
public class AdminAppFormInterviewController {

    private static Logger log = LoggerFactory.getLogger(AdminAppFormInterviewController.class.getName());

    private InterviewService interviewService = ServiceFactory.getInterviewService();
    private ApplicationFormService applicationFormService = ServiceFactory.getApplicationFormService();

    @RequestMapping(value = "getApplicationForm/{studentId}", method = RequestMethod.POST)
    public String getApplicationForm(@PathVariable Long studentId) {
        log.info("Getting Application Form by student Id");
        ApplicationForm applicationForm = applicationFormService.getCurrentApplicationFormByUserId(studentId);
        Gson applicationFormGson = GsonFactory.getApplicationFormGson();
        return applicationFormGson.toJson(applicationForm);
    }

    @RequestMapping(value = "getOldApplicationForms/{studentId}", method = RequestMethod.POST)
    public List<String> getOldApplicationForms(@PathVariable Long studentId) {
        log.info("Getting old Application Forms by student Id");
        List<ApplicationForm> allApplicationForms = applicationFormService.getOldApplicationFormsByUserId(studentId);
        Gson applicationFormGson = GsonFactory.getApplicationFormGson();
        List<String> appForms = new ArrayList<>();
        for (ApplicationForm appForm : allApplicationForms) {
            String jsonResult = applicationFormGson.toJson(appForm);
            appForms.add(jsonResult);
        }
        return appForms;
    }


    @RequestMapping(value = "getRolesInterview/{applicationFormId}", method = RequestMethod.GET)
    public Set<Role> getInterviewRoles(@PathVariable Long applicationFormId) {
        log.info("Getting Roles of Staff that assigned specified application form");
        ApplicationForm applicationForm = applicationFormService.getApplicationFormById(applicationFormId);
        List<Interview> interviews = interviewService.getByApplicationForm(applicationForm);
        Set<Role> interviewRoles = new HashSet<>();
        for (Interview interview : interviews) {
            interviewRoles.add(interview.getRole());
        }
        return interviewRoles;
    }

    @RequestMapping(value = "getInterview/{applicationFormId}/{role}", method = RequestMethod.GET)
    public String getInterview(@PathVariable Long applicationFormId, @PathVariable Long role) {
        log.info("Getting Interview data by application form and role of interviewer");
        Interview interview = null;
        ApplicationForm applicationForm = applicationFormService.getApplicationFormById(applicationFormId);
        List<Interview> interviews = interviewService.getByApplicationForm(applicationForm);
        for (Interview i : interviews) {
            if (i.getRole().getId().equals(role)) {
                interview = i;
            }
        }
        Gson interviewGson = GsonFactory.getInterviewGson();
        return interviewGson.toJson(interview);
    }

    @RequestMapping(value = "getAdequateMark/{applicationFormId}", method = RequestMethod.GET)
    public boolean getAdequateMark(@PathVariable Long applicationFormId) {
        log.info("Getting adequate mark for admin by application form");
        return interviewService.haveNonAdequateMarkForAdmin(applicationFormId);
    }

    @RequestMapping(value = "appForm/{applicationFormId}", method = RequestMethod.GET)
    public void exportAppform(@PathVariable Long applicationFormId, HttpServletResponse response) throws IOException, DocumentException {
        log.info("Getting adequate mark for admin by application form");
        ApplicationForm applicationForm = applicationFormService.getApplicationFormById(applicationFormId);
        ExportApplicationForm pdfAppForm = new ExportApplicationFormImp();
        response.setHeader("Content-Disposition", "inline; filename=ApplicationForm.pdf");
        response.setContentType("application/pdf");
        pdfAppForm.export(applicationForm, response);
    }
}
