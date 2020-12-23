package uk.co.ceilingcat.rrd.gateways.emailoutputgateway

import arrow.core.Either
import arrow.core.Either.Companion.left
import arrow.core.Either.Companion.right
import uk.co.ceilingcat.rrd.entities.ServiceDetails
import uk.co.ceilingcat.rrd.usecases.UpcomingOutputGateway
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

data class EmailBodyText(val text: String)
data class EmailFrom(val address: InternetAddress)
data class EmailPassword(val text: String)
data class EmailTo(val address: InternetAddress)
data class EmailUserName(val text: String)
data class SubjectTemplate(val text: String)

/**
 * The errors that `EMailOutputGateway.notify(...)` may return, contained in a `Left(.)`
 */
sealed class EMailOutputGatewayException : Throwable() {
    object NotifyException : EMailOutputGatewayException()
}

typealias NotifyError = EMailOutputGatewayException.NotifyException

/**
 * Construct a new email output gateway.
 *
 * Mails are sent using SMTP, using a hardcoded provider.
 *
 * The `subjectTemplate` permits the replacement of a single token of the value `<<serviceType>>'; it is
 * replaced with: `'Refuse'` when the service type is `ServiceType.REFUSE` and, `'Recycling'`, when the service is of type
 * `ServiceType.RECYCLING`.
 *
 * @param emailUserName the SMTP user's name
 * @param emailPassword the SMTP user's password
 * @param emailFrom the email's source mailbox name
 * @param emailTo the email's destination mailbox name
 * @param emailBodyText the email's body text
 * @param subjectTemplate the email's subject template.
 */
fun createEMailOutputGateway(
    emailUserName: EmailUserName,
    emailPassword: EmailPassword,
    emailFrom: EmailFrom,
    emailTo: EmailTo,
    emailBodyText: EmailBodyText,
    subjectTemplate: SubjectTemplate
): UpcomingOutputGateway =
    EMailOutputGateway(emailUserName, emailPassword, emailFrom, emailTo, emailBodyText, subjectTemplate)

private class EMailOutputGateway(
    private val emailUserName: EmailUserName,
    private val emailPassword: EmailPassword,
    private val emailFrom: EmailFrom,
    private val emailTo: EmailTo,
    private val emailBodyText: EmailBodyText,
    private val subjectTemplate: SubjectTemplate
) : UpcomingOutputGateway {

    override fun notify(serviceDetails: ServiceDetails): Either<NotifyError, Unit> =
        try {
            right(
                Transport.send(
                    buildMessage(
                        getSession(buildSessionProperties()),
                        serviceDetails
                    )
                )
            )
        } catch (t: Throwable) {
            left(NotifyError)
        }

    private fun getSession(mailSessionProperties: Properties) =
        Session.getInstance(
            mailSessionProperties,
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication =
                    PasswordAuthentication(emailUserName.text, emailPassword.text)
            }
        )

    private fun buildMessage(session: Session, serviceDetails: ServiceDetails) =
        MimeMessage(session).apply {
            setFrom(emailFrom.address)
            setRecipients(Message.RecipientType.TO, listOf(emailTo.address).toTypedArray())
            subject = makeSubject(serviceDetails)
            setText(emailBodyText.text)
        }

    private fun makeSubject(serviceDetails: ServiceDetails) = replaceTokens(
        listOf(Pair("serviceType", serviceDetails.type.toString().toLowerCase().capitalize())),
        subjectTemplate.text
    )

    companion object {
        private const val MAIL_PREFIX = "mail."
        private const val SMTP_PREFIX = "smtp."

        private fun mailProp(s: Any) = "$MAIL_PREFIX$s"

        private fun replaceTokens(tokensAndReplacements: List<Pair<String, String>>, templateText: String) =
            tokensAndReplacements.fold(templateText) { acc, (first, second) ->
                acc.replace(
                    buildToken(first),
                    second
                )
            }

        private fun buildToken(tokenText: String) = "<<$tokenText>>"

        private fun smtpProp(s: Any) = "$SMTP_PREFIX$s"

        private val sessionProperties: Map<String, Any> = mapOf(
            "debug" to true,
            smtpProp("auth") to true,
            smtpProp("starttls.required") to true,
            smtpProp("host") to "smtp.mail.me.com",
            smtpProp("port") to 587,
        )

        private fun buildSessionProperties() = sessionProperties
            .entries
            .fold(Properties()) { props, (k, v) ->
                props[mailProp(k)] = v
                props
            }
    }
}
