package com.ptsecurity.appsec.ai.desktop.utils.xml.report;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ptsecurity.appsec.ai.desktop.utils.xml.report.details.Report;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@DisplayName("Check PT AI Desktop XML reports helper")
class ReportTest {

    @Test
    @DisplayName("Parse correct XML reports")
    void parseCorrectXmlReports() {
        Assertions.assertDoesNotThrow(() -> {
            testXmlParse("xml/report.app01.xml");
            testXmlParse("xml/report.app02.xml");
        });
    }

    Report testXmlParse(String xmlFile) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(xmlFile);
        String xml = IOUtils.toString(is, StandardCharsets.UTF_8);
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Report report = xmlMapper.readValue(xml, Report.class);
        return report;
    }
}