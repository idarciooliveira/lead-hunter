package me.iofdev.leadhunter.api;

/** A create hit a slug that is already taken. */
class CampaignExistsException extends RuntimeException {

    CampaignExistsException(String message) {
        super(message);
    }
}
