package java_first_pojo.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java_first_pojo.dao.enitity.AlarmInfo;
import java_first_pojo.dao.enitity.AuditResponse;
import java_first_pojo.dao.enitity.AuditResult;
import java_first_pojo.dao.enitity.FlowInfo;
import java_first_pojo.dao.enitity.HourAudit;
import java_first_pojo.dao.enitity.NorResponse;
import java_first_pojo.dao.enitity.ServiceDelInfo;
import java_first_pojo.dao.enitity.ServiceInfo;
import java_first_pojo.dao.enitity.ServiceStatus;
import java_first_pojo.dao.enitity.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class ServiceImpl implements IService {

    private final Logger logger = LoggerFactory.getLogger(ServiceImpl.class);

    @Override
    public NorResponse addService(ServiceInfo serviceInfo) {
        logger.info(new Gson().toJson(serviceInfo, new TypeToken<ServiceInfo>() {
        }.getType()));
        return justTest();
    }

    @Override
    public NorResponse modService(ServiceInfo serviceInfo) {
        return justTest();
    }

    @Override
    public NorResponse delService(ServiceDelInfo serviceDelInfo) {
        return justTest();
    }

    @Override
    public NorResponse updateServiceStatus(ServiceStatus serviceStatus) {
        return justTest();
    }

    @Override
    public AuditResponse hourAudit(HourAudit hourAudit) {
        AuditResponse auditResponse = new AuditResponse();
        auditResponse.setIdentification("GXPT");
        auditResponse.setVersion("1.0");
        List<AuditResult> auditResults = new ArrayList<>();
        AuditResult auditResult = new AuditResult();
        try {
            auditResponse.setIp(InetAddress.getLocalHost().getHostAddress());
            auditResult.setResult("ok");
        } catch (UnknownHostException e) {
            auditResponse.setIp("127.0.0.1");
            auditResult.setResult("error");
            auditResult.setDesc("20001");
        }
        auditResults.add(auditResult);
        auditResponse.setAuditResults(auditResults);
        return auditResponse;
    }

    @Override
    public NorResponse serviceFlowAuditInfo(FlowInfo flowInfo) {
        return justTest();
    }

    @Override
    public NorResponse serviceAlarmAuditInfo(AlarmInfo alarmInfo) {
        return justTest();
    }

    @Override
    public NorResponse reportTest(TestInfo testInfo) {
        return justTest();
    }

    private NorResponse justTest() {
        NorResponse norResponse = new NorResponse();
        norResponse.setIdentification("GXPT");
        norResponse.setVersion("1.0");
        try {
            norResponse.setIp(InetAddress.getLocalHost().getHostAddress());
            norResponse.setResult("ok");
        } catch (UnknownHostException e) {
            norResponse.setResult("error");
            norResponse.setIp("127.0.0.1");
            norResponse.setDesc(e.getMessage());
        }
        return norResponse;
    }
}
