package org.motechproject.outbox.api;


import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.motechproject.context.EventContext;
import org.motechproject.event.EventRelay;
import org.motechproject.model.MotechEvent;
import org.motechproject.outbox.api.dao.OutboundVoiceMessageDao;
import org.motechproject.outbox.api.model.OutboundVoiceMessage;
import org.motechproject.outbox.api.model.OutboundVoiceMessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.Calendar;
import java.util.List;

//TODO: The API of VoiceOutboxService is anemic (exposes the domain model, just a wrapper over the dao)
public class VoiceOutboxServiceImpl implements VoiceOutboxService {

    final Logger log = LoggerFactory.getLogger(VoiceOutboxServiceImpl.class);
    private int numDayskeepSavedMessages;

    /**
     * Should be configurable externally.
     * if pendingMessages > maxNumberOfPendingMessages we need to emit event
     */
    private int maxNumberOfPendingMessages;

    @Autowired(required = false)
    private EventRelay eventRelay = EventContext.getInstance().getEventRelay();

    @Autowired
    private OutboundVoiceMessageDao outboundVoiceMessageDao;

    @Override
    @SuppressWarnings("unchecked")
    public void addMessage(OutboundVoiceMessage outboundVoiceMessage) {

        log.info("Add message: " + outboundVoiceMessage);

        if (outboundVoiceMessage == null) {
            throw new IllegalArgumentException("OutboundVoiceMessage can not be null.");
        }
        outboundVoiceMessageDao.add(outboundVoiceMessage);

        //sends max-pending-messages event if needed
        String pId = outboundVoiceMessage.getPartyId();
        Assert.hasText(pId, "VoiceMessage must have a valid partyId");
        int msgNum = outboundVoiceMessageDao.getPendingMessagesCount(pId);
        if (maxNumberOfPendingMessages == msgNum) {
            log.warn(String.format("Max number (%d) of pending messages reached!", msgNum));
            eventRelay.sendEventMessage(new MotechEvent(EventKeys.OUTBOX_MAX_PENDING_MESSAGES_EVENT_SUBJECT, ArrayUtils.toMap(new Object[][]{{EventKeys.PARTY_ID_KEY, pId}})));
        }
    }

    @Override
    public OutboundVoiceMessage getNextPendingMessage(String partyId) {

        log.info("Get next pending message for the party ID: " + partyId);

        if (partyId == null || partyId.isEmpty()) {
            throw new IllegalArgumentException("Party ID can not be null or empty.");
        }

        OutboundVoiceMessage nextPendingVoiceMessage = null;
        List<OutboundVoiceMessage> pendingVoiceMessages = outboundVoiceMessageDao.getPendingMessages(partyId);

        if (pendingVoiceMessages.size() > 0) {

            nextPendingVoiceMessage = pendingVoiceMessages.get(0);
        }

        return nextPendingVoiceMessage;
    }

    @Override
    public OutboundVoiceMessage getNextSavedMessage(String partyId) {

        log.info("Get next saved message for the party ID: " + partyId);

        if (partyId == null || partyId.isEmpty()) {
            throw new IllegalArgumentException("Party ID can not be null or empty.");
        }

        OutboundVoiceMessage nextSavedVoiceMessage = null;
        List<OutboundVoiceMessage> savedVoiceMessages = outboundVoiceMessageDao.getSavedMessages(partyId);

        if (savedVoiceMessages.size() > 0) {

            nextSavedVoiceMessage = savedVoiceMessages.get(0);
        }

        return nextSavedVoiceMessage;
    }

    @Override
    public OutboundVoiceMessage getMessageById(String outboundVoiceMessageId) {

        log.info("Get message by ID: " + outboundVoiceMessageId);

        if (outboundVoiceMessageId == null || outboundVoiceMessageId.isEmpty()) {
            throw new IllegalArgumentException("outboundVoiceMessageId can not be null or empty.");
        }
        return outboundVoiceMessageDao.get(outboundVoiceMessageId);
    }

    @Override
    public void removeMessage(String outboundVoiceMessageId) {

        log.info("Remove message ID: " + outboundVoiceMessageId);

        if (outboundVoiceMessageId == null || outboundVoiceMessageId.isEmpty()) {
            throw new IllegalArgumentException("outboundVoiceMessageId can not be null or empty.");
        }
        OutboundVoiceMessage outboundVoiceMessage = getMessageById(outboundVoiceMessageId);

        outboundVoiceMessageDao.remove(outboundVoiceMessage);
    }

    @Override
    public void setMessageStatus(String outboundVoiceMessageId, OutboundVoiceMessageStatus status) {

        log.info("Set status: " + status + " to the message ID: " + outboundVoiceMessageId);

        if (outboundVoiceMessageId == null || outboundVoiceMessageId.isEmpty()) {
            throw new IllegalArgumentException("outboundVoiceMessageId can not be null or empty.");
        }

        OutboundVoiceMessage outboundVoiceMessage = getMessageById(outboundVoiceMessageId);
        outboundVoiceMessage.setStatus(status);

        outboundVoiceMessageDao.update(outboundVoiceMessage);
    }

    @Override
    public void saveMessage(String outboundVoiceMessageId) {

        log.info("Save in the outbox message ID: " + outboundVoiceMessageId);

        if (outboundVoiceMessageId == null || outboundVoiceMessageId.isEmpty()) {
            throw new IllegalArgumentException("outboundVoiceMessageId can not be null or empty.");
        }

        OutboundVoiceMessage outboundVoiceMessage = getMessageById(outboundVoiceMessageId);
        outboundVoiceMessage.setStatus(OutboundVoiceMessageStatus.SAVED);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, numDayskeepSavedMessages);
        outboundVoiceMessage.setExpirationDate(calendar.getTime());

        outboundVoiceMessageDao.update(outboundVoiceMessage);
    }

    @Override
    public int getNumberPendingMessages(String partyId) {

        log.info("Get number of pending messages for the party ID: " + partyId);

        if (partyId == null || partyId.isEmpty()) {
            throw new IllegalArgumentException("Party ID can not be null or empty.");
        }

        return outboundVoiceMessageDao.getPendingMessagesCount(partyId);
    }

    @Override
    public int getNumDayskeepSavedMessages() {
        return numDayskeepSavedMessages;
    }

    @Override
    public void setNumDayskeepSavedMessages(int numDayskeepSavedMessages) {
        this.numDayskeepSavedMessages = numDayskeepSavedMessages;
    }

    @Override
    public void setMaxNumberOfPendingMessages(int maxNumberOfPendingMessages) {
        this.maxNumberOfPendingMessages = maxNumberOfPendingMessages;
    }

    @Override
    public int getMaxNumberOfPendingMessages() {
        return this.maxNumberOfPendingMessages;
    }

    @Override
    public OutboundVoiceMessage nextMessage(String lastMessageId, String partyId) {
        if(StringUtils.isNotEmpty(lastMessageId))
            setMessageStatus(lastMessageId, OutboundVoiceMessageStatus.PLAYED);
        return getNextPendingMessage(partyId);
    }
}
