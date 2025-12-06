const API_BASE_URL = "http://localhost:8080/api/rooms";

export const createRoomApi = async (): Promise<string | null> => {
  try {
    const response = await fetch(`${API_BASE_URL}/create`, {
      method: "POST",
    });
    
    if (response.ok) {
      const data = await response.json();
      return data.roomId; 
    }
    return null;
  } catch (error) {
    console.error("Lỗi khi tạo phòng:", error);
    return null;
  }
};

export const checkRoomExistsApi = async (roomId: string): Promise<boolean> => {
  try {
    const response = await fetch(`${API_BASE_URL}/${roomId}/join`, {
      method: "POST", 
    });
    return response.ok; 
  } catch (error) {
    console.error("Lỗi khi kiểm tra phòng:", error);
    return false;
  }
};