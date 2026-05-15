# QuickBite Frontend

QuickBite is an online food delivery platform frontend built with modern web technologies to provide a fast, responsive, and seamless user experience for ordering food.

## 🚀 Tech Stack

- **Framework:** React 18
- **Build Tool:** Vite
- **Routing:** React Router DOM v6
- **Testing:** Vitest & React Testing Library

## 📦 Getting Started

### Prerequisites

- Node.js (v18 or higher recommended)
- npm or yarn

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Harshit-Jain01/OnlineFoodDeliveryPlatform-Frontend.git
   cd OnlineFoodDeliveryPlatform-Frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Configure Environment Variables:
   - Copy `.env.example` to `.env`
   - Update the configuration values as needed for your local setup.

4. Start the development server:
   ```bash
   npm run dev
   ```

## 🛠️ Scripts

- `npm run dev`: Starts the local development server.
- `npm run build`: Builds the app for production to the `dist` folder.
- `npm run preview`: Locally previews the production build.
- `npm run test`: Runs the test suite with coverage.
- `npm run test:watch`: Runs tests in watch mode.

## 🌿 Branching Strategy

This repository follows a structured branching strategy:

- `main`: The stable production-ready branch.
- `dev`: The integration branch for new features before they hit `main`.
- `feature/*`: Working branches for implementing specific features.

## 🤝 Contributing

1. Create a feature branch from `dev` (`git checkout -b feature/your-feature-name`)
2. Commit your changes (`git commit -m "feat: your commit message"`)
3. Push to the branch (`git push origin feature/your-feature-name`)
4. Open a Pull Request targeting the `dev` branch.
