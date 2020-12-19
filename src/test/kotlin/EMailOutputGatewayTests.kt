package uk.co.ceilingcat.rrd.gateways.emailoutputgateway

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import javax.mail.internet.InternetAddress

@TestInstance(PER_CLASS)
internal class EMailOutputGatewayTests {

    @Test
    fun `That we can create gateway instances with createEMailOutputGateway()`() {
        val emailUserName = EmailUserName("mailbox@domain.com")
        val emailPassword = EmailPassword("asdjf39ru4fsj")
        val emailFrom = EmailFrom(InternetAddress("mailbox@domain.com"))
        val emailTo = EmailTo(InternetAddress("mailbox@domain.com"))
        val emailBodyText = EmailBodyText("email body text")
        val subjectTemplate = SubjectTemplate("email subject")

        Assertions.assertNotNull(
            createEMailOutputGateway(
                emailUserName,
                emailPassword,
                emailFrom,
                emailTo,
                emailBodyText,
                subjectTemplate
            )
        )
    }
}
