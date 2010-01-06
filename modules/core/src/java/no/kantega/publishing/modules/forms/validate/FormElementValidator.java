/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package no.kantega.publishing.modules.forms.validate;

import java.util.List;
import no.kantega.publishing.modules.forms.model.FormValue;

public interface FormElementValidator {
    public String getId();

    public List<FormError> validate(FormValue formValue, List<FormError> formErrors);
}
