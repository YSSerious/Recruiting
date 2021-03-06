package ua.kpi.nc.controller.admin;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.kpi.nc.persistence.dto.MessageDto;
import ua.kpi.nc.persistence.dto.StaffFiltrationParamsDto;
import ua.kpi.nc.persistence.dto.UserDto;
import ua.kpi.nc.persistence.dto.UserRateDto;
import ua.kpi.nc.persistence.model.*;
import ua.kpi.nc.persistence.model.impl.real.RoleImpl;
import ua.kpi.nc.persistence.model.impl.real.UserImpl;
import ua.kpi.nc.service.*;
import ua.kpi.nc.controller.auth.PasswordEncoderGeneratorService;
import ua.kpi.nc.service.util.SenderService;
import ua.kpi.nc.service.util.SenderServiceImpl;

import javax.mail.MessagingException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static ua.kpi.nc.persistence.model.enums.EmailTemplateEnum.STAFF_INTERVIEW_SELECT;
import static ua.kpi.nc.persistence.model.enums.EmailTemplateEnum.STAFF_REGISTRATION;
import static ua.kpi.nc.persistence.model.enums.RoleEnum.*;

/**
 * Created by dima on 23.04.16.
 */
@RestController
@RequestMapping("/admin")
public class AdminManagementStaffController {

    private static Logger log = LoggerFactory.getLogger(AdminManagementStaffController.class);

    private UserService userService = ServiceFactory.getUserService();

    private RoleService roleService = ServiceFactory.getRoleService();

    private InterviewService interviewService = ServiceFactory.getInterviewService();

    private SenderService senderService = SenderServiceImpl.getInstance();

    private PasswordEncoderGeneratorService passwordEncoderGeneratorService = PasswordEncoderGeneratorService.getInstance();

    private UserTimePriorityService userTimePriorityService = ServiceFactory.getUserTimePriorityService();

    private RecruitmentService recruitmentService = ServiceFactory.getRecruitmentService();

    private EmailTemplateService emailTemplateService = ServiceFactory.getEmailTemplateService();

    private final static String NEED_MORE_SOFT  = "Need select more then 0 soft";

    private final static String NEED_MORE_TECH = "Need select more then 0 tech";

    private final static String NEED_MORE_TECH_SOFT = "Need select more then 0 then and soft";

    private final static String TIME_PRIORITY_ALREADY_EXIST = "Time priority already exist";

    private final static String CAN_NOT_DELETE = "Can't remove assignet user";

    private final static String CAN_NOT_EDIT = "Cannot edit employee.";


    @RequestMapping(value = "showAllEmployees", method = RequestMethod.GET)
    public List<User> showEmployees(@RequestParam int pageNum, @RequestParam Long rowsNum, @RequestParam Long sortingCol,
                                    @RequestParam boolean increase) {
        return userService.getEmployeesFromToRows(calculateStartRow(pageNum, rowsNum), rowsNum, sortingCol, increase);
    }

    @RequestMapping(value = "showFilteredEmployees", method = RequestMethod.POST)
    public List<User> showFilteredEmployees(@RequestBody StaffFiltrationParamsDto staffFiltrationParamsDto) {
        List<Role> neededRoles = new ArrayList<>();
        for(Long roleId: staffFiltrationParamsDto.getRolesId())
            neededRoles.add(roleService.getRoleById(roleId));

        Long fromRow = calculateStartRow(staffFiltrationParamsDto.getPageNum(),staffFiltrationParamsDto.getRowsNum());

        return userService.getFilteredEmployees(fromRow, staffFiltrationParamsDto.getRowsNum(),
                staffFiltrationParamsDto.getSortingCol(), staffFiltrationParamsDto.isIncrease(),
                staffFiltrationParamsDto.getIdStart(), staffFiltrationParamsDto.getIdFinish(),
                neededRoles, staffFiltrationParamsDto.isInterviewer(),
                staffFiltrationParamsDto.isNotInterviewer(), staffFiltrationParamsDto.isNotEvaluated());
    }

    private Long calculateStartRow(int pageNum, Long rowsNum) {
        return (pageNum - 1) * rowsNum;
    }

    @RequestMapping(value = "getCountOfEmployee", method = RequestMethod.GET)
    public Long getCountOfEmployee() {
        return userService.getAllEmployeeCount();
    }

    @RequestMapping(value = "getCountOfEmployeeFiltered", method = RequestMethod.POST)
    public Long getCountOfEmployeeFiltered(@RequestBody StaffFiltrationParamsDto staffFiltrationParamsDto) {
        List<Role> neededRoles = new ArrayList<>();
        for(Long roleId: staffFiltrationParamsDto.getRolesId())
            neededRoles.add(roleService.getRoleById(roleId));

        Long fromRow = calculateStartRow(staffFiltrationParamsDto.getPageNum(),staffFiltrationParamsDto.getRowsNum());

        return userService.getAllEmployeeCountFiltered(fromRow, staffFiltrationParamsDto.getRowsNum(),
                staffFiltrationParamsDto.getSortingCol(), staffFiltrationParamsDto.isIncrease(),
                staffFiltrationParamsDto.getIdStart(), staffFiltrationParamsDto.getIdFinish(),
                neededRoles, staffFiltrationParamsDto.isInterviewer(), staffFiltrationParamsDto.isNotInterviewer(),
                staffFiltrationParamsDto.isNotEvaluated());
    }

    @RequestMapping(value = "search", method = RequestMethod.POST)
    public List<User> searchEmployee(@RequestParam String lastName) {
        return userService.getEmployeesByNameFromToRows(lastName);
    }


    @RequestMapping(value = "addEmployee", method = RequestMethod.POST)
    public void addEmployee(@RequestBody UserDto userDto) throws MessagingException {
        List<RoleImpl> roles = userDto.getRoleList();
        List<Role> userRoles = new ArrayList<>();
        for (Role role : roles) {
            userRoles.add(roleService.getRoleByTitle(role.getRoleName()));
        }
        Date date = new Date();
        String password = RandomStringUtils.randomAlphabetic(10);
        User user = new UserImpl();
        user.setEmail(userDto.getEmail());
        user.setFirstName(userDto.getFirstName());
        user.setSecondName(userDto.getSecondName());
        user.setLastName(userDto.getLastName());
        user.setPassword(passwordEncoderGeneratorService.encode(password));
        user.setActive(true);
        user.setRegistrationDate(new Timestamp(date.getTime()));
        userService.insertUser(user, userRoles);
        user.setPassword(password);
        EmailTemplate emailTemplate = emailTemplateService.getById(STAFF_REGISTRATION.getId());
        String template = emailTemplateService.showTemplateParams(emailTemplate.getText(), user);
        senderService.send(user.getEmail(), emailTemplate.getTitle(), template);
    }


    @RequestMapping(value = "editEmployee", method = RequestMethod.POST)
    public ResponseEntity editEmployeeParams(@RequestBody UserDto userDto) {
        User user = userService.getUserByID(userDto.getId());
        user.setFirstName(userDto.getFirstName());
        user.setSecondName(userDto.getSecondName());
        user.setLastName(userDto.getLastName());
        user.setEmail(userDto.getEmail());
        Set<Role> userRoles = new HashSet<>();
        for (Role role : userDto.getRoleList()) {
            userRoles.add(roleService.getRoleByTitle("ROLE_" + role.getRoleName()));
        }
        user.setRoles(userRoles);
        if (userService.updateUserWithRole(user)){
            return ResponseEntity.ok().body(null);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(CAN_NOT_EDIT);
        }
    }

    @RequestMapping(value = "changeEmployeeStatus", method = RequestMethod.GET)
    public Boolean changeEmployeeStatus(@RequestParam String email) {
        User user = userService.getUserByUsername(email);
        user.setActive(!user.isActive());
        userService.updateUser(user);
        return user.isActive();
    }

    @RequestMapping(value = "showAssignedStudent", method = RequestMethod.POST)
    public List<UserRateDto> showAssignedStudent(@RequestParam String email) {
        User user = userService.getUserByUsername(email);
        List<UserRateDto> userRateDtos = new ArrayList<>();
        for (Interview interview : interviewService.getByInterviewer(user)) {
            UserRateDto userRateDto = new UserRateDto(interview.getApplicationForm().getUser(),
                    interview.getMark(),
                    interview.getRole(),
                    interview.getId());
            userRateDtos.add(userRateDto);
        }
        return userRateDtos;
    }

    @RequestMapping(value = "deleteEmployee", method = RequestMethod.GET)
    public void deleteEmployee(@RequestParam String email) {
        User user = userService.getUserByUsername(email);
        userService.deleteUser(user);
    }

    @RequestMapping(value = "deleteAssignedStudent", method = RequestMethod.POST)
    public ResponseEntity deleteAssignedStudent(@RequestParam String idInterview) {
        Interview interview = interviewService.getById(Long.parseLong(idInterview));
        if (null == interview) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(CAN_NOT_DELETE);
        } else {
            interviewService.deleteInterview(interview);
            return ResponseEntity.ok().body(null);
        }
    }

    @RequestMapping(value = "getActiveEmployee", method = RequestMethod.GET)
    public List<Long> getActiveEmployee() {

        Long activeTech = userService.getCountActiveEmployees(ROLE_TECH.getId(),
                ROLE_SOFT.getId());

        Long activeSoft = userService.getCountActiveEmployees(ROLE_SOFT.getId(),
                ROLE_TECH.getId());

        Long activeSoftTech = userService.getCountActiveDoubleRoleEmployee();

        List<Long> activeEmployees = new ArrayList<>();
        activeEmployees.add(activeTech);
        activeEmployees.add(activeSoft);
        activeEmployees.add(activeSoftTech);
        return activeEmployees;
    }


    @RequestMapping(value = "getMaxId", method = RequestMethod.GET)
    public Long getMaxId() {
        return userService.getUserCount();
    }

    @RequestMapping(value = "hasNotMarked", method = RequestMethod.POST)
    public List<String> hasNotMarked() {
        return userService.getNotMarkedInterviwers();
    }

    @RequestMapping(value = "confirmStaff", method = RequestMethod.GET)
    public ResponseEntity confirmStaff() {
        if (userTimePriorityService.isSchedulePrioritiesExistStaff()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageDto(TIME_PRIORITY_ALREADY_EXIST));
        } else {
            List<User> softList = userService.getActiveStaffByRole(new RoleImpl(ROLE_SOFT.getId()));
            List<User> techList = userService.getActiveStaffByRole(new RoleImpl(ROLE_TECH.getId()));
            if (softList.isEmpty() & techList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageDto(NEED_MORE_TECH_SOFT));
            } else if (softList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageDto(NEED_MORE_SOFT));
            } else if (techList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageDto(NEED_MORE_TECH));
            } else {
                Set<User> staff = new LinkedHashSet<>(softList);
                staff.addAll(techList);
                EmailTemplate emailTemplate = emailTemplateService.getById(STAFF_INTERVIEW_SELECT.getId());
                for (User user : staff) {
                    String template = emailTemplateService.showTemplateParams(emailTemplate.getText(), user);
                    try {
                        senderService.send(user.getEmail(), emailTemplate.getTitle(), template);
                    } catch (MessagingException e) {
                        log.error("Cannot send email {}", e);
                    }
                }
                userTimePriorityService.createStaffTimePriorities(staff);
                return ResponseEntity.ok(null);
            }
        }
    }

}
