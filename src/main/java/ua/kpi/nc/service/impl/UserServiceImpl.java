package ua.kpi.nc.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.kpi.nc.persistence.dao.DataSourceSingleton;
import ua.kpi.nc.persistence.dao.UserDao;
import ua.kpi.nc.persistence.model.Role;
import ua.kpi.nc.persistence.model.ScheduleTimePoint;
import ua.kpi.nc.persistence.model.User;
import ua.kpi.nc.service.UserService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

/**
 * Created by Chalienko on 13.04.2016.
 */

public class UserServiceImpl implements UserService {

    private static Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private UserDao userDao;

    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public User getUserByUsername(String username) {
        return userDao.getByUsername(username);
    }

    @Override
    public User getUserByID(Long id) {
        return userDao.getByID(id);
    }

    @Override
    public boolean isExist(String username) {
        return userDao.isExist(username);
    }

    @Override
    public boolean insertUser(User user, Role role) {
        try(Connection connection = DataSourceSingleton.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            Long generatedUserId = userDao.insertUser(user, connection);
            user.setId(generatedUserId);
            userDao.addRole(user,role,connection);
            connection.commit();
        } catch (SQLException e) {
            if (log.isWarnEnabled()) {
                log.error("Cannot insert user",e);
            }
            return false;
        }
        return true;
    }

    @Override
    public int updateUser(User user){
        return userDao.updateUser(user);
    }

    @Override
    public boolean addRole(User user, Role role) {
        return userDao.addRole(user, role);
    }

    @Override
    public int deleteRole(User user, Role role) {
        return userDao.deleteRole(user, role);
    }

    @Override
    public int deleteUser(User user) {
        return userDao.deleteUser(user);
    }

    @Override
    public Long insertFinalTimePoint(User user, ScheduleTimePoint scheduleTimePoint) {
        return userDao.insertFinalTimePoint(user,scheduleTimePoint);
    }

    @Override
    public int deleteFinalTimePoint(User user, ScheduleTimePoint scheduleTimePoint) {
        return userDao.deleteFinalTimePoint(user,scheduleTimePoint);
    }

    @Override
    public Set<User> getUsersByToken(String token) {
        return userDao.getUsersByToken(token);
    }

    @Override
    public Set<User> getAssignedStudents(Long id) {
        return userDao.getAssignedStudents(id);
    }

    @Override
    public Set<User> getAllStudents() {
        return userDao.getAllStudents();
    }

    @Override
    public Set<User> getAllEmploees() {
        return userDao.getAllEmploees();
    }

    @Override
    public Set<User> getAll(){ return userDao.getAll();}
}
