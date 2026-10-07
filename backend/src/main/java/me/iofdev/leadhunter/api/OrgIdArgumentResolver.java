package me.iofdev.leadhunter.api;

import me.iofdev.leadhunter.auth.OrgId;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Gives controller methods the {@link OrgId} that {@link OrgMembershipFilter} checked. */
@Component
class OrgIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == OrgId.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container, NativeWebRequest request,
                                  WebDataBinderFactory binderFactory) {
        Object org = request.getAttribute(OrgMembershipFilter.ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (org == null) {
            throw new IllegalStateException("no organization on this request");
        }
        return org;
    }
}
