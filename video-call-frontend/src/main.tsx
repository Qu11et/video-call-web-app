import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom';
import './App.css'
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import { Provider } from 'react-redux' 
import { store } from './store/store'  

createRoot(document.getElementById('root')!).render(
  <Provider store={store}> {/* <--- Bọc Provider */}
    {/* <StrictMode> */}
    <BrowserRouter>
      <App />
    </BrowserRouter>
    {/* </StrictMode> */}
  </Provider>,
)
