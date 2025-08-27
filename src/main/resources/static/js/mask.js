
export function maskEmail(email) {
    const [localPart, domain] = email.split('@');
    const maskedLocalPart = localPart.length > 3
        ? localPart.substring(0, 3) + '***'
        : localPart.substring(0, 1) + '***';
    return `${maskedLocalPart}@${domain}`;
}

export function maskPhone(phone) {
    if (!phone || phone.length < 11) return '***'; // 안전장치
    const lastFourDigits = phone.slice(-4);
    const firstThreeDigits = phone.slice(0, 3);
    return `${firstThreeDigits}-****-${lastFourDigits}`;
}