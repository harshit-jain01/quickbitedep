import { fireEvent, render, screen } from "@testing-library/react";
import AuthForm from "./AuthForm";

function buildProps(overrides = {}) {
  return {
    fields: [
      { name: "email", label: "Email", type: "email", required: true },
      {
        name: "role",
        label: "Role",
        type: "select",
        required: true,
        options: [
          { value: "CUSTOMER", label: "Customer" },
          { value: "AGENT", label: "Agent" }
        ]
      }
    ],
    values: { email: "", role: "CUSTOMER" },
    errors: {},
    onChange: vi.fn(),
    onBlur: vi.fn(),
    onSubmit: vi.fn((e) => e.preventDefault()),
    submitLabel: "Continue",
    isSubmitting: false,
    isSubmitDisabled: false,
    apiError: "",
    ...overrides
  };
}

describe("AuthForm", () => {
  it("renders input/select fields and propagates user events", () => {
    const props = buildProps();
    render(<AuthForm {...props} />);

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "user@q.com" } });
    fireEvent.blur(screen.getByLabelText("Email"));
    fireEvent.change(screen.getByLabelText("Role"), { target: { value: "AGENT" } });

    expect(props.onChange).toHaveBeenCalledTimes(2);
    expect(props.onBlur).toHaveBeenCalledTimes(1);
  });

  it("shows field errors and api error banner", () => {
    render(
      <AuthForm
        {...buildProps({
          errors: { email: "Email is required" },
          apiError: "Invalid credentials"
        })}
      />
    );

    expect(screen.getByText("Email is required")).toBeInTheDocument();
    expect(screen.getByText("Invalid credentials")).toBeInTheDocument();
  });

  it("disables submit and shows loading label when submitting", () => {
    const props = buildProps({ isSubmitting: true, isSubmitDisabled: true });
    render(<AuthForm {...props} />);

    const submitButton = screen.getByRole("button", { name: "Please wait..." });
    expect(submitButton).toBeDisabled();
  });

  it("submits form when user clicks submit", () => {
    const props = buildProps();
    render(<AuthForm {...props} />);

    const form = screen.getByRole("button", { name: "Continue" }).closest("form");
    expect(form).not.toBeNull();
    fireEvent.submit(form);

    expect(props.onSubmit).toHaveBeenCalledTimes(1);
  });
});
