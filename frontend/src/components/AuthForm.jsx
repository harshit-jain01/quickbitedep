export default function AuthForm({
  fields,
  values,
  errors,
  onChange,
  onBlur,
  onSubmit,
  submitLabel,
  isSubmitting,
  isSubmitDisabled,
  apiError,
  leadingContent,
  footer
}) {
  return (
    <form className="auth-form" onSubmit={onSubmit}>
      {leadingContent}

      {fields.map((field) => (
        <label className="field" key={field.name}>
          <span>{field.label}</span>
          {(() => {
            const hasError = Boolean(errors[field.name]);
            const controlStyle = hasError ? { borderColor: "#ef4444" } : undefined;
            if (field.type === "select") {
              return (
                <select
                  name={field.name}
                  value={values[field.name] || ""}
                  onChange={onChange}
                  onBlur={onBlur}
                  required={field.required}
                  style={controlStyle}
                >
                  {field.options?.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              );
            }
            return (
              <input
                name={field.name}
                type={field.type || "text"}
                placeholder={field.placeholder}
                value={values[field.name] || ""}
                onChange={onChange}
                onBlur={onBlur}
                required={field.required}
                minLength={field.minLength}
                autoComplete={field.autoComplete}
                style={controlStyle}
              />
            );
          })()}
          <small style={{ color: "#ef4444", fontSize: "0.8rem", minHeight: "1rem", display: "block" }}>
            {errors[field.name] || " "}
          </small>
        </label>
      ))}

      {apiError ? <div className="error-banner">{apiError}</div> : null}

      <button className="primary-button" type="submit" disabled={isSubmitting || isSubmitDisabled}>
        {isSubmitting ? "Please wait..." : submitLabel}
      </button>

      {footer}
    </form>
  );
}
